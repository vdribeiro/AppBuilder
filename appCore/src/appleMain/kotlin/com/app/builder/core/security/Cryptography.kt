package com.app.builder.core.security

import kotlin.io.encoding.Base64
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.CoreCrypto.kCCKeySizeAES256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.posix.arc4random_buf
import platform.posix.size_tVar
import com.app.builder.App
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.developmentMode
import com.app.builder.core.telemetry.Telemetry

actual suspend fun encrypt(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        val secretKey = getSecretKey()
        memScoped {
            val inputData = content.encodeToByteArray()

            val iv = ByteArray(size = IV_LENGTH)
            arc4random_buf(__buf = iv.refTo(index = 0), __nbytes = IV_LENGTH.toULong())

            val bufferSize = inputData.size + BLOCK_SIZE.toInt()
            val buffer = ByteArray(size = bufferSize)
            val numBytesEncrypted = alloc<size_tVar>()

            val status = CCCrypt(
                op = kCCEncrypt,
                alg = ALGORITHM,
                options = PADDING,
                key = secretKey.refTo(index = 0),
                keyLength = KEY_SIZE.toULong(),
                iv = iv.refTo(index = 0),
                dataIn = inputData.takeIf { it.isNotEmpty() }?.refTo(index = 0),
                dataInLength = inputData.size.toULong(),
                dataOut = buffer.refTo(index = 0),
                dataOutAvailable = bufferSize.toULong(),
                dataOutMoved = numBytesEncrypted.ptr
            )
            if (status != kCCSuccess) error(message = "CCCrypt encrypt failed: $status")

            val encryptedBytes = buffer.copyOf(newSize = numBytesEncrypted.value.toInt())
            val macData = iv + encryptedBytes
            val hmac = ByteArray(size = HMAC_LENGTH)
            CCHmac(
                algorithm = HMAC_ALGORITHM,
                key = secretKey.refTo(index = 0),
                keyLength = KEY_SIZE.toULong(),
                data = macData.refTo(index = 0),
                dataLength = macData.size.toULong(),
                macOut = hmac.refTo(index = 0)
            )
            Base64.encode(source = iv + encryptedBytes + hmac)
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to encrypt", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

actual suspend fun decrypt(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        val secretKey = getSecretKey()
        memScoped {
            val combined = Base64.decode(source = content)
            if (combined.size < IV_LENGTH + HMAC_LENGTH) error(message = "Invalid length")

            val iv = combined.copyOfRange(fromIndex = 0, toIndex = IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(fromIndex = IV_LENGTH, toIndex = combined.size - HMAC_LENGTH)
            val storedHmac = combined.copyOfRange(fromIndex = combined.size - HMAC_LENGTH, toIndex = combined.size)

            val macData = iv + encryptedBytes
            val computedHmac = ByteArray(size = HMAC_LENGTH)
            CCHmac(
                algorithm = HMAC_ALGORITHM,
                key = secretKey.refTo(index = 0),
                keyLength = KEY_SIZE.toULong(),
                data = macData.refTo(index = 0),
                dataLength = macData.size.toULong(),
                macOut = computedHmac.refTo(index = 0)
            )
            var diff = 0
            for (i in computedHmac.indices) diff = diff or (computedHmac[i].toInt() xor storedHmac[i].toInt())
            if (computedHmac.size != storedHmac.size || diff != 0) error(message = "HMAC verification failed")

            val bufferSize = encryptedBytes.size + BLOCK_SIZE.toInt()
            val buffer = ByteArray(size = bufferSize)
            val numBytesDecrypted = alloc<size_tVar>()

            val decStatus = CCCrypt(
                op = kCCDecrypt,
                alg = ALGORITHM,
                options = PADDING,
                key = secretKey.refTo(index = 0),
                keyLength = KEY_SIZE.toULong(),
                iv = iv.refTo(index = 0),
                dataIn = encryptedBytes.refTo(index = 0),
                dataInLength = encryptedBytes.size.toULong(),
                dataOut = buffer.refTo(index = 0),
                dataOutAvailable = bufferSize.toULong(),
                dataOutMoved = numBytesDecrypted.ptr
            )
            if (decStatus != kCCSuccess) error(message = "CCCrypt decrypt failed: $decStatus")

            buffer.copyOf(newSize = numBytesDecrypted.value.toInt()).decodeToString()
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to decrypt", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

/**
 * Get the Secret Key from [keyStorage], or generate and store one when none is stored.
 * A key is only generated when the storage reports that none exists; any other read failure throws, so an existing key is never replaced by a new one that cannot decrypt what was encrypted before.
 * A freshly generated key that the storage refuses to keep is rejected rather than returned, so callers never encrypt with a key that cannot be recovered to decrypt again.
 *
 * @return the secret key or throw on error.
 */
private suspend fun getSecretKey(): ByteArray = withContext(context = Dispatcher.IO) {
    runCatching {
        val account = "${App.id}.masterkey"
        val stored = keyStorage.read(account = account)
        if (stored != null) {
            if (stored.size != KEY_SIZE.toInt()) error(message = "Stored secret key has an invalid length: ${stored.size}")
            return@runCatching stored
        }
        ByteArray(size = KEY_SIZE.toInt())
            .apply { arc4random_buf(__buf = refTo(index = 0), __nbytes = KEY_SIZE.toULong()) }
            .also { keyStorage.add(account = account, key = it) }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to get secret key", throwable = it)
    }.getOrThrow()
}

actual suspend fun hash(content: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        memScoped {
            val inputData = content.encodeToByteArray()
            val hash = UByteArray(size = CC_SHA256_DIGEST_LENGTH)
            CC_SHA256(
                data = inputData.takeIf { it.isNotEmpty() }?.refTo(index = 0),
                len = inputData.size.toUInt(),
                md = hash.refTo(index = 0)
            )
            hash.joinToString(separator = "") { it.toString(radix = 16).padStart(length = 2, padChar = '0') }
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to hash", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
}

actual suspend fun deriveKey(password: String, salt: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        require(value = salt.isNotEmpty()) { "Salt must not be empty" }
        memScoped {
            val saltBytes = salt.encodeToByteArray().toUByteArray()
            val derived = UByteArray(size = DERIVED_KEY_SIZE)
            val status = CCKeyDerivationPBKDF(
                algorithm = kCCPBKDF2,
                password = password,
                passwordLen = password.encodeToByteArray().size.toULong(),
                salt = saltBytes.refTo(index = 0),
                saltLen = saltBytes.size.toULong(),
                prf = kCCPRFHmacAlgSHA256,
                rounds = DERIVATION_ROUNDS,
                derivedKey = derived.refTo(index = 0),
                derivedKeyLen = derived.size.toULong()
            )
            if (status != kCCSuccess) error(message = "Unable to derive key: $status")
            derived.joinToString(separator = "") { it.toString(radix = 16).padStart(length = 2, padChar = '0') }
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to derive key", throwable = it)
    }.getOrNull()
}

private const val TAG = "Cryptography"

/** The length, in bytes, of the initialization vector used for AES encryption. */
private const val IV_LENGTH = 16
/** The length, in bytes, of the HMAC appended to the ciphertext for integrity verification. */
private const val HMAC_LENGTH = 32
/** The size, in bytes, of the AES secret key. */
private const val KEY_SIZE = kCCKeySizeAES256
/** The AES block size, in bytes, used to size the encryption output buffer. */
private const val BLOCK_SIZE = kCCBlockSizeAES128
/** The HMAC algorithm used to authenticate the encrypted payload. */
private const val HMAC_ALGORITHM = kCCHmacAlgSHA256
/** The symmetric cipher algorithm used to encrypt and decrypt content. */
private const val ALGORITHM = kCCAlgorithmAES
/** The padding scheme applied to the plaintext before encryption. */
private const val PADDING = kCCOptionPKCS7Padding
/** The number of PBKDF2 rounds. */
private const val DERIVATION_ROUNDS = 210_000u
/** The size, in bytes, of the derived key. */
private const val DERIVED_KEY_SIZE = 32
