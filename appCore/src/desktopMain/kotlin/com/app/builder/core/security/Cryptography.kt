package com.app.builder.core.security

import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
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
 * Get the [SecretKeySpec] from [KeyStore] or generate one if it does not exist.
 *
 * @return the secret key spec or throw on error.
 */
private suspend fun getSecretKey(): SecretKeySpec = withContext(context = Dispatcher.IO) {
    runCatching {
        val keyStoreFile = File(appDataPath, "security.p12")
        val password = when (platform.os) {
            // Reads the unique machine id
            OS.Linux -> (File("/var/lib/dbus/machine-id").takeIf { it.exists() } ?: File("/etc/machine-id").takeIf { it.exists() })
                ?.readText()
                .orEmpty()
                .trim()
                .toCharArray()
            // Reads the MachineGuid from the Windows Registry
            OS.Windows -> ProcessBuilder("reg", "query", "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid")
                .start()
                .inputStream
                .bufferedReader()
                .readText()
                .substringAfter(delimiter = "REG_SZ")
                .trim()
                .toCharArray()
            // Reads the IOPlatformUUID from the macOS I/O Kit registry
            OS.Mac -> ProcessBuilder("bash", "-c", "ioreg -rd1 -c IOPlatformExpertDevice | grep IOPlatformUUID")
                .start()
                .inputStream
                .bufferedReader()
                .readText()
                .split("\"")
                .getOrNull(index = 3)
                .orEmpty()
                .trim()
                .toCharArray()

            else -> CharArray(size = 0)
        }.takeIf { it.isNotEmpty() } ?: error(message = "Unable to get password")
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            if (keyStoreFile.exists()) {
                keyStoreFile.inputStream().use { load(it, password) }
            } else load(null, password)
        }
        val passwordProtection = KeyStore.PasswordProtection(password)
        val entry = keyStore.getEntry(ALIAS, passwordProtection) as? KeyStore.SecretKeyEntry
        val secretKey = entry?.secretKey ?: run {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            keyGenerator.init(KEY_SIZE)
            keyGenerator.generateKey().also {
                // Save secret key
                keyStore.setEntry(ALIAS, KeyStore.SecretKeyEntry(it), passwordProtection)
                keyStoreFile.outputStream().use { stream -> keyStore.store(stream, password) }
            }
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