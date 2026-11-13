package com.app.builder.core.security

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlin.io.encoding.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import android.security.keystore.KeyProperties
import com.app.builder.App
import com.app.builder.core.flow.Dispatcher

internal actual suspend fun encryptContent(content: String): String {
    val cipher = Cipher.getInstance(SPEC)
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

    val encryptedBytes = cipher.doFinal(content.encodeToByteArray())
    return Base64.encode(source = cipher.iv + encryptedBytes)
}

internal actual suspend fun decryptContent(content: String): String {
    val combined = Base64.decode(source = content)
    val encryptedBytes = combined.copyOfRange(fromIndex = IV_LENGTH_BYTE, toIndex = combined.size)

    val iv = combined.copyOfRange(fromIndex = 0, toIndex = IV_LENGTH_BYTE)
    val cipher = Cipher.getInstance(SPEC)
    val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), parameterSpec)
    return cipher.doFinal(encryptedBytes).decodeToString()
}

/** Serializes the key lookup and generation within this process. */
private val secretKeyMutex = Mutex()

/**
 * Get the [SecretKey] from [keyStorage], or generate one when none is stored.
 * The lookup and the generation run under [secretKeyMutex], so concurrent callers get the same key.
 *
 * @return the secret key or throw on error.
 */
private suspend fun getSecretKey(): SecretKey = withContext(context = Dispatcher.IO) {
    val alias = ALIAS
    secretKeyMutex.withLock { keyStorage.read(alias = alias) ?: keyStorage.generate(alias = alias) }
}

internal actual suspend fun sha256(content: String): ByteArray = MessageDigest.getInstance(HASH_ALGORITHM).digest(content.encodeToByteArray())

internal actual suspend fun pbkdf2(password: String, salt: String, rounds: Int, size: Int): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt.encodeToByteArray(), rounds, size)
    return try {
        SecretKeyFactory.getInstance(DERIVATION_ALGORITHM).generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

/** The length, in bits, of the GCM authentication tag appended to the ciphertext. */
private const val TAG_LENGTH_BIT = 128
/** The length, in bytes, of the initialization vector used for AES-GCM encryption. */
private const val IV_LENGTH_BYTE = 12
/** The alias under which the secret key is stored in [keyStorage]. */
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
/** The key derivation function used to turn a password into a verifier. */
private const val DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
