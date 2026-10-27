@file:ExcludeFromTesting

package com.app.builder.core.security

import java.io.File
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import com.app.builder.App
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.platform.platform
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.test.ExcludeFromTesting

/** The last secret read from the operating system's secret store, kept so the store is only queried again after a failure. */
@Volatile
private var cachedSecret: String? = null

/**
 * Returns the secret held by the operating system's secret store, generating and storing a new one when it holds none.
 * A successful read is cached for the lifetime of the process, while a failed one is retried on the next call.
 *
 * @return The secret, or null where there is no secret store or it cannot hold one.
 */
internal fun getKeystoreSecret(): String? =
    cachedSecret ?: getSecretStore()?.let { getOrCreateSecret(store = it) }?.also { cachedSecret = it }

/**
 * Returns the secret held by [store], generating and storing a new one when it holds none.
 * The generated secret is read back before being accepted, so a store that silently drops it resolves to null rather than to a value that cannot be recovered on the next run.
 *
 * @param store The secret store to use.
 * @return The secret, or null when the store cannot hold one.
 */
private fun getOrCreateSecret(store: SecretStore): String? = runCatching {
    store.read()?.takeIf { it.isNotEmpty() } ?: run {
        val generated = Base64.encode(source = ByteArray(size = SECRET_LENGTH_BYTE).apply { SecureRandom().nextBytes(this) })
        if (!store.write(secret = generated)) return@run null
        store.read()?.takeIf { it == generated }
    }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get secret from the store", throwable = it)
}.getOrNull()

/**
 * Returns the secret store of the running operating system.
 * Each store needs where it files the secret: the app data directory on Windows, and the app id on macOS and Linux.
 *
 * @return The store, or null where there is none or the app has no identity to file a secret under.
 */
private fun getSecretStore(): SecretStore? =
    when (platform.os) {
        OS.Windows -> appDataPath.takeIf { it.isNotBlank() }?.let { WindowsSecretStore(directory = it) }
        OS.Mac -> App.id.takeIf { it.isNotBlank() }?.let { AppleSecretStore(appId = it) }
        OS.Linux -> App.id.takeIf { it.isNotBlank() }?.let { LinuxSecretStore(appId = it) }?.takeIf { it.isAvailable() }
        else -> null
    }

/** A place the operating system keeps a secret on behalf of the current user. */
private interface SecretStore {
    /** Returns the stored secret, or null when there is none. */
    fun read(): String?

    /**
     * Stores [secret], replacing any previous value.
     *
     * @return `true` when the secret was stored.
     */
    fun write(secret: String): Boolean
}

/**
 * A file encrypted by the Windows Data Protection API under the current user account, written and read through PowerShell.
 *
 * @property directory The app data directory the encrypted file is kept in.
 */
@ExcludeFromTesting
private class WindowsSecretStore(private val directory: String): SecretStore {
    private val file: File get() = File(directory, "security.dpapi")

    override fun read(): String? {
        val blob = runCatching { file.takeIf { it.exists() }?.readText()?.trim() }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        return run(
            command = listOf(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "[Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR((ConvertTo-SecureString -String '$blob')))"
            )
        )?.trim()?.takeIf { it.isNotEmpty() }
    }

    override fun write(secret: String): Boolean {
        val blob = run(
            command = listOf(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "ConvertFrom-SecureString -SecureString (ConvertTo-SecureString -String '$secret' -AsPlainText -Force)"
            )
        )?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        return runCatching { file.writeText(text = blob) }.isSuccess
    }
}

/**
 * The macOS login keychain, reached through the `security` command line tool.
 *
 * @property appId The application identifier the secret is filed under, as the keychain item's service.
 */
@ExcludeFromTesting
private class AppleSecretStore(private val appId: String): SecretStore {

    override fun read(): String? = run(
        command = listOf("security", "find-generic-password", "-w", "-s", appId, "-a", ACCOUNT)
    )?.trim()?.takeIf { it.isNotEmpty() }

    override fun write(secret: String): Boolean = run(
        command = listOf("security", "add-generic-password", "-U", "-s", appId, "-a", ACCOUNT, "-w", secret)
    ) != null
}

/**
 * The Linux Secret Service, reached through the `secret-tool` command line tool.
 *
 * @property appId The application identifier the secret is filed under, as the item's service attribute and label.
 */
@ExcludeFromTesting
private class LinuxSecretStore(private val appId: String): SecretStore {

    /** Whether `secret-tool` is installed and its keyring reachable. */
    fun isAvailable(): Boolean = run(command = listOf("secret-tool", "lookup", "service", appId, "attribute", "probe")) != null

    override fun read(): String? = run(
        command = listOf("secret-tool", "lookup", "service", appId, "account", ACCOUNT)
    )?.trim()?.takeIf { it.isNotEmpty() }

    override fun write(secret: String): Boolean = run(
        command = listOf("secret-tool", "store", "--label=$appId", "service", appId, "account", ACCOUNT),
        input = secret
    ) != null
}

/**
 * Runs [command] and collects its output.
 *
 * @param command The command and its arguments.
 * @param input The text to write to the process standard input, if any.
 * @return The standard output of the command, or null when it fails, times out, or exits with a non-zero status.
 */
private fun run(command: List<String>, input: String? = null): String? = runCatching {
    val process = ProcessBuilder(command).redirectErrorStream(false).start()
    input?.let { process.outputStream.bufferedWriter().use { writer -> writer.write(it) } } ?: process.outputStream.close()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        return null
    }
    output.takeIf { process.exitValue() == 0 }
}.getOrNull()

private const val TAG = "Keychain"

/** The account the secret is filed under. */
private const val ACCOUNT = "master_key"
/** The length, in bytes, of a generated secret. */
private const val SECRET_LENGTH_BYTE = 32
/** How long to wait for a secret store command before giving up. */
private const val PROCESS_TIMEOUT_SECONDS = 10L
