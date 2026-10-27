package com.app.builder.core.security

import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.app.builder.App
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.platform.developmentMode
import com.app.builder.core.platform.machineId
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.withFileLock
import com.app.builder.data.storage.writeAtomically

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

/** The secret key and the alias it was loaded under, kept once loaded so the keystore is only opened again after a failure or an alias change. */
@Volatile
private var cachedSecretKey: Pair<String, SecretKeySpec>? = null

/** Serializes keystore access within this process; the lock file in [loadSecretKey] does the same across processes. */
private val secretKeyMutex = Mutex()

/**
 * Returns the [SecretKeySpec], loading it from the [KeyStore] on first use.
 * A successful load is cached for the lifetime of the process, while a failed one is retried on the next call.
 * The cache only serves the alias it was loaded under, so a change of [App.id] loads the key again.
 *
 * @return the secret key spec or throw on error.
 */
private suspend fun getSecretKey(): SecretKeySpec {
    val alias = ALIAS
    cachedSecretKey?.takeIf { it.first == alias }?.let { return it.second }
    return secretKeyMutex.withLock {
        cachedSecretKey?.takeIf { it.first == alias }?.second ?: loadSecretKey(alias = alias).also { cachedSecretKey = alias to it }
    }
}

/**
 * 
 */
private fun File.load(keyStore: KeyStore, password: CharArray) = runCatching { inputStream().use { keyStore.load(it, password) } }

/**
 * Get the [SecretKeySpec] from [KeyStore], generating the key only when the keystore does not exist yet.
 * The keystore is unlocked with a random secret held by the operating system's secret store, falling back to the machine id where there is none.
 * A new secret is only generated while no keystore is locked by another one: when the keystore does not exist yet, or is still locked by the machine id.
 * A keystore still locked by the machine id is reopened with it and written back under the stored secret.
 * An existing keystore without a key under [alias] fails rather than getting a new key, which would leave everything encrypted before unreadable.
 * The whole load runs under a lock file, so two processes never create or rewrite the keystore at the same time, and the keystore is only ever replaced atomically.
 *
 * @param alias The alias the key is stored under.
 * @return the secret key spec or throw on error.
 */
private suspend fun loadSecretKey(alias: String): SecretKeySpec = withContext(context = Dispatcher.IO) {
    runCatching {
        withFileLock(file = File(appDataPath, "security.lock")) {
            val keyStoreFile = File(appDataPath, "security.p12")
            val keyStoreExists = keyStoreFile.exists()
            val machineId = machineId()
            val canCreateSecret = !keyStoreExists || (machineId != null && keyStoreFile.load(keyStore = KeyStore.getInstance("PKCS12"), password = machineId).isSuccess)
            val secret = readKeystoreSecret() ?: if (canCreateSecret) createKeystoreSecret() else null
            val password = secret?.toCharArray() ?: machineId ?: error(message = "Unable to get a keystore password")

            val keyStore = KeyStore.getInstance("PKCS12")
            val legacyPassword = when {
                !keyStoreExists -> null.also { keyStore.load(null, password) }
                keyStoreFile.load(keyStore = keyStore, password = password).isSuccess -> null
                machineId != null && keyStoreFile.load(keyStore = keyStore, password = machineId).isSuccess -> machineId
                else -> error(message = "Unable to open the keystore")
            }

            val entry = keyStore.getEntry(alias, KeyStore.PasswordProtection(legacyPassword ?: password)) as? KeyStore.SecretKeyEntry
            val secretKey = when {
                entry != null -> entry.secretKey
                !keyStoreExists -> KeyGenerator.getInstance(ALGORITHM).apply { init(KEY_SIZE) }.generateKey()
                else -> error(message = "The keystore holds no key under $alias")
            }
            if (!keyStoreExists || legacyPassword != null) {
                keyStore.setEntry(alias, KeyStore.SecretKeyEntry(secretKey), KeyStore.PasswordProtection(password))
                keyStoreFile.writeAtomically { stream -> keyStore.store(stream, password) }
            }
            SecretKeySpec(secretKey.encoded, ALGORITHM)
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
