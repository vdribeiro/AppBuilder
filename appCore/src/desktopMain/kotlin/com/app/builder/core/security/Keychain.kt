@file:ExcludeFromTesting

package com.app.builder.core.security

import java.io.File
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import com.app.builder.App
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.platform.platform
import com.app.builder.core.platform.runCommand
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.writeAtomically
import com.app.builder.test.ExcludeFromTesting

/**
 * Reads the keystore secret from the operating system's secret store.
 *
 * @return The secret, or null where there is no secret store, it holds none, or it cannot be read.
 */
internal fun readKeystoreSecret(): String? = runCatching {
    getSecretStore()?.read()?.takeIf { it.isNotEmpty() }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to read the secret from the store", throwable = it)
}.getOrNull()

/**
 * Generates a new keystore secret and stores it in the operating system's secret store, replacing any previous one.
 * The generated secret is read back before being accepted, so a store that silently drops it resolves to null rather than to a value that cannot be recovered on the next run.
 *
 * @return The new secret, or null where there is no secret store, or it cannot hold one.
 */
internal fun createKeystoreSecret(): String? = runCatching {
    val store = getSecretStore() ?: return null
    val generated = Base64.encode(source = ByteArray(size = SECRET_LENGTH_BYTE).apply { SecureRandom().nextBytes(this) })
    if (!store.write(secret = generated)) return null
    store.read()?.takeIf { it == generated }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to create a secret in the store", throwable = it)
}.getOrNull()

/**
 * Returns the secret store of the running operating system.
 *
 * @return The store, or null where there is none, or the app has no identity to file a secret under.
 */
private fun getSecretStore(): SecretStore? =
    when (platform.os) {
        OS.Windows -> appDataPath.takeIf { it.isNotBlank() }?.let { WindowsSecretStore(directory = it) }
        OS.Mac -> App.id.takeIf { it.isNotBlank() }?.let { AppleSecretStore(appId = it) }
        OS.Linux -> App.id.takeIf { it.isNotBlank() }?.let { LinuxSecretStore(appId = it) }
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
        return runCommand(
            command = listOf(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "[Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR((ConvertTo-SecureString -String '$blob')))"
            )
        )?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Reads the secret from standard input, so it never appears in the command line or in PowerShell script logging. */
    override fun write(secret: String): Boolean {
        val blob = runCommand(
            command = listOf(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "ConvertFrom-SecureString -SecureString (ConvertTo-SecureString -String ([Console]::In.ReadToEnd().Trim()) -AsPlainText -Force)"
            ),
            input = secret
        )?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        return runCatching { file.writeAtomically { stream -> stream.write(blob.encodeToByteArray()) } }.isSuccess
    }
}

/**
 * The macOS login keychain, reached through the `security` command line tool.
 *
 * @property appId The application identifier the secret is filed under, as the keychain item's service.
 */
@ExcludeFromTesting
private class AppleSecretStore(private val appId: String): SecretStore {

    override fun read(): String? = runCommand(
        command = listOf("security", "find-generic-password", "-w", "-s", appId, "-a", ACCOUNT)
    )?.trim()?.takeIf { it.isNotEmpty() }

    /** Sends the command through `security -i` on standard input, so the secret never appears in the process arguments. Its outcome is confirmed by reading the secret back. */
    override fun write(secret: String): Boolean = runCommand(
        command = listOf("security", "-i"),
        input = "add-generic-password -U -s $appId -a $ACCOUNT -w $secret\n"
    ) != null
}

/**
 * The Linux Secret Service, reached through the `secret-tool` command line tool.
 *
 * @property appId The application identifier the secret is filed under, as the item's service attribute and label.
 */
@ExcludeFromTesting
private class LinuxSecretStore(private val appId: String): SecretStore {

    override fun read(): String? = runCommand(
        command = listOf("secret-tool", "lookup", "service", appId, "account", ACCOUNT)
    )?.trim()?.takeIf { it.isNotEmpty() }

    override fun write(secret: String): Boolean = runCommand(
        command = listOf("secret-tool", "store", "--label=$appId", "service", appId, "account", ACCOUNT),
        input = secret
    ) != null
}

private const val TAG = "Keychain"

/** The account the secret is filed under. */
private const val ACCOUNT = "master_key"
/** The length, in bytes, of a generated secret. */
private const val SECRET_LENGTH_BYTE = 32
