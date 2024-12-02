package com.app.builder.core.security

import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64
import kotlinx.coroutines.withContext
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.app.builder.App
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.developmentMode
import com.app.builder.core.telemetry.Telemetry

actual suspend fun encrypt(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        val cipher = Cipher.getInstance(SPEC)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

        val encryptedBytes = cipher.doFinal(content.encodeToByteArray())
        Base64.encode(source = cipher.iv + encryptedBytes)
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
 * Get the [SecretKey] from [KeyStore] or generate one if it does not exist.
 *
 * @return the secret key spec or throw on error.
 */
private suspend fun getSecretKey(): SecretKey = withContext(context = Dispatcher.IO) {
    runCatching {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        val key = keyStore.getKey(ALIAS, null) as? SecretKey
        key ?: run {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM, PROVIDER)
            val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setKeySize(KEY_SIZE)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
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
/** The [KeyStore] provider used to store the secret key in the Android Keystore system. */
private const val PROVIDER = "AndroidKeyStore"
/** The alias under which the secret key is stored in the [KeyStore]. */
private val ALIAS: String get() = "${App.id}.master_key"
/** The symmetric cipher algorithm used to encrypt and decrypt content. */
private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
/** The block mode used for AES encryption. */
private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
/** The padding scheme applied to the plaintext before encryption. */
private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
/** The cipher transformation string identifying the algorithm, block mode, and padding. */
private const val SPEC = "$ALGORITHM/$BLOCK_MODE/$PADDING"
/** The digest algorithm used to compute content hashes. */
private const val HASH_ALGORITHM = KeyProperties.DIGEST_SHA256
