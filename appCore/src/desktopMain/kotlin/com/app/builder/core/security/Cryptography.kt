package com.app.builder.core.security

import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlinx.coroutines.withContext
import com.app.builder.App
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.platform.developmentMode
import com.app.builder.core.platform.platform
import com.app.builder.core.telemetry.Telemetry

actual suspend fun encrypt(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        val iv = ByteArray(size = IV_LENGTH_BYTE).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance(SPEC)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), parameterSpec)

        val encryptedBytes = cipher.doFinal(content.encodeToByteArray())
        Base64.encode(source = iv + encryptedBytes)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to encrypt", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

actual suspend fun decrypt(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        val combined = Base64.decode(source = content)
        val encryptedBytes = combined.copyOfRange(fromIndex = IV_LENGTH_BYTE, toIndex = combined.size)

        val iv = combined.copyOfRange(fromIndex = 0, toIndex = IV_LENGTH_BYTE)
        val cipher = Cipher.getInstance(SPEC)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec)
        cipher.doFinal(encryptedBytes).decodeToString()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to decrypt", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

/**
 * Reads the identifier the operating system assigns to this machine.
 *
 * @return The machine id, or null when it cannot be read.
 */
private fun machineId(): CharArray? = runCatching {
    when (platform.os) {
        OS.Windows -> ProcessBuilder("reg", "query", "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid")
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .substringAfter(delimiter = "REG_SZ")
            .trim()

        OS.Mac -> ProcessBuilder("bash", "-c", "ioreg -rd1 -c IOPlatformExpertDevice | grep IOPlatformUUID")
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .split("\"")
            .getOrNull(index = 3)
            .orEmpty()
            .trim()

        OS.Linux -> (File("/var/lib/dbus/machine-id").takeIf { it.exists() } ?: File("/etc/machine-id").takeIf { it.exists() })
            ?.readText()
            .orEmpty()
            .trim()

        else -> ""
    }.takeIf { it.isNotEmpty() }?.toCharArray()
}.onFailure {
    Telemetry.error(tag = TAG, message = "Unable to read the machine id", throwable = it)
}.getOrNull()

private fun File.load(keyStore: KeyStore, password: CharArray) = runCatching { inputStream().use { keyStore.load(it, password) } }

/**
 * Get the [SecretKeySpec] from [KeyStore] or generate one if it does not exist.
 * The keystore is unlocked with a random secret held by the operating system's secret store, falling back to the machine id where there is none.
 * A keystore still locked by the machine id is reopened with it and written back under the stored secret.
 *
 * @return the secret key spec or throw on error.
 */
private suspend fun getSecretKey(): SecretKeySpec = withContext(context = Dispatcher.IO) {
    runCatching {
        val keyStoreFile = File(appDataPath, "security.p12")
        val machineId = machineId()
        val password = getKeystoreSecret()?.toCharArray() ?: machineId ?: error(message = "Unable to get a keystore password")

        val keyStore = KeyStore.getInstance("PKCS12")
        val legacyPassword = when {
            !keyStoreFile.exists() -> null.also { keyStore.load(null, password) }
            keyStoreFile.load(keyStore = keyStore, password = password).isSuccess -> null
            machineId != null && keyStoreFile.load(keyStore = keyStore, password = machineId).isSuccess -> machineId
            else -> error(message = "Unable to open the keystore")
        }

        val protection = KeyStore.PasswordProtection(password)
        val entry = keyStore.getEntry(ALIAS, KeyStore.PasswordProtection(legacyPassword ?: password)) as? KeyStore.SecretKeyEntry
        val secretKey = entry?.secretKey ?: KeyGenerator.getInstance(ALGORITHM).apply { init(KEY_SIZE) }.generateKey()
        if (entry == null || legacyPassword != null) {
            keyStore.setEntry(ALIAS, KeyStore.SecretKeyEntry(secretKey), protection)
            keyStoreFile.outputStream().use { stream -> keyStore.store(stream, password) }
        }
        SecretKeySpec(secretKey.encoded, ALGORITHM)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get secret key", throwable = it)
    }.getOrThrow()
}

actual suspend fun hash(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = digest.digest(content.encodeToByteArray())
        hashBytes.joinToString(separator = "") { it.toUByte().toString(radix = 16).padStart(length = 2, padChar = '0') }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to hash", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

actual suspend fun deriveKey(password: String, salt: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        val spec = PBEKeySpec(password.toCharArray(), salt.encodeToByteArray(), DERIVATION_ROUNDS, DERIVED_KEY_SIZE)
        SecretKeyFactory.getInstance(DERIVATION_ALGORITHM)
            .generateSecret(spec)
            .encoded
            .joinToString(separator = "") { it.toUByte().toString(radix = 16).padStart(length = 2, padChar = '0') }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to derive key", throwable = it)
    }.getOrNull()
}

private const val TAG = "Cryptography"

/** The length, in bits, of the GCM authentication tag appended to the ciphertext. */
private const val TAG_LENGTH_BIT = 128
/** The length, in bytes, of the initialization vector used for AES-GCM encryption. */
private const val IV_LENGTH_BYTE = 12
/** The size, in bits, of the AES secret key. */
private const val KEY_SIZE = 256
/** The alias under which the secret key is stored in the [KeyStore]. */
private val ALIAS: String get() = "${App.id}.master_key"
/** The symmetric cipher algorithm used to encrypt and decrypt content. */
private const val ALGORITHM = "AES"
/** The cipher transformation string identifying the algorithm, block mode, and padding. */
private const val SPEC = "$ALGORITHM/GCM/NoPadding"
/** The digest algorithm used to compute content hashes. */
private const val HASH_ALGORITHM = "SHA-256"
/** The key derivation function used to turn a password into a verifier. */
private const val DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
/** The number of PBKDF2 rounds. */
private const val DERIVATION_ROUNDS = 210_000
/** The size, in bits, of the derived key. */
private const val DERIVED_KEY_SIZE = 256
