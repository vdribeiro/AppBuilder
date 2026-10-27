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

/** A place the operating system keeps a secret on behalf of the current user. */
internal interface SecretStore {
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
 * Returns the secret held by [store], generating and storing a new one when it holds none.
 * The generated secret is read back before being accepted, so a store that silently drops it resolves to null rather than to a value that cannot be recovered on the next run.
 *
 * @param store The secret store to use.
 * @return The secret, or null when the store cannot hold one.
 */
internal fun getOrCreateSecret(store: SecretStore): String? = runCatching {
    store.read()?.takeIf { it.isNotEmpty() } ?: run {
        val generated = Base64.encode(source = ByteArray(size = SECRET_LENGTH_BYTE).apply { SecureRandom().nextBytes(this) })
        if (!store.write(secret = generated)) return@run null
        store.read()?.takeIf { it == generated }
    }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to get secret from the store", throwable = it)
}.getOrNull()

/** The secret store of the running operating system, or null where there is none or the app has no identity to file a secret under. */
internal val secretStore: SecretStore? by lazy {
    if (App.id.isBlank()) return@lazy null
    when (platform.os) {
        OS.Windows -> WindowsSecretStore()
        OS.Mac -> AppleSecretStore()
        OS.Linux -> LinuxSecretStore().takeIf { it.isAvailable() }
        else -> null
    }
}

/** A file encrypted by the Windows Data Protection API under the current user account, written and read through PowerShell. */
@ExcludeFromTesting
private class WindowsSecretStore: SecretStore {
    private val file: File get() = File(appDataPath, "security.dpapi")

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

/** The macOS login keychain, reached through the `security` command line tool. */
@ExcludeFromTesting
private class AppleSecretStore: SecretStore {

    override fun read(): String? = run(
        command = listOf("security", "find-generic-password", "-w", "-s", App.id, "-a", ACCOUNT)
    )?.trim()?.takeIf { it.isNotEmpty() }

    override fun write(secret: String): Boolean = run(
        command = listOf("security", "add-generic-password", "-U", "-s", App.id, "-a", ACCOUNT, "-w", secret)
    ) != null
}

/** The Linux Secret Service, reached through the `secret-tool` command line tool. */
@ExcludeFromTesting
private class LinuxSecretStore: SecretStore {

    /** Whether `secret-tool` is installed and its keyring reachable. */
    fun isAvailable(): Boolean = run(command = listOf("secret-tool", "lookup", "service", App.id, "attribute", "probe")) != null

    override fun read(): String? = run(
        command = listOf("secret-tool", "lookup", "service", App.id, "account", ACCOUNT)
    )?.trim()?.takeIf { it.isNotEmpty() }

    override fun write(secret: String): Boolean = run(
        command = listOf("secret-tool", "store", "--label=${App.id}", "service", App.id, "account", ACCOUNT),
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
@ExcludeFromTesting
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
