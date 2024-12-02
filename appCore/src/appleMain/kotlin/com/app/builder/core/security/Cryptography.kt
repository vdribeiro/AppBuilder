package com.app.builder.core.security

import kotlin.io.encoding.Base64
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.CoreCrypto.kCCKeySizeAES256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreCrypto.kCCSuccess
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.arc4random_buf
import platform.posix.memcpy
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
                dataIn = inputData.refTo(index = 0),
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
 * Get the Secret Key or generate one if it does not exist.
 *
 * @return the secret key or throw on error.
 */
private suspend fun getSecretKey(): ByteArray = withContext(context = Dispatcher.IO) {
    runCatching {
        memScoped {
            val account = "${App.id}.masterkey"
            val cfAccount = CFStringCreateWithCString(alloc = null, cStr = account, encoding = kCFStringEncodingUTF8)
            val query = CFDictionaryCreateMutable(allocator = null, capacity = 4, keyCallBacks = null, valueCallBacks = null)
            CFDictionaryAddValue(theDict = query, key = kSecClass, value = kSecClassGenericPassword)
            CFDictionaryAddValue(theDict = query, key = kSecAttrAccount, value = cfAccount)
            CFDictionaryAddValue(theDict = query, key = kSecReturnData, value = kCFBooleanTrue)
            CFDictionaryAddValue(theDict = query, key = kSecMatchLimit, value = kSecMatchLimitOne)
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query = query, result = result.ptr)

            if (status == errSecSuccess) {
                @Suppress("UNCHECKED_CAST") val data = result.value as CFDataRef
                val bytes = CFDataGetBytePtr(theData = data)
                val length = CFDataGetLength(theData = data)
                ByteArray(size = length.toInt()).apply {
                    usePinned { pinned ->
                        memcpy(__dst = pinned.addressOf(0), __src = bytes, __n = length.toULong())
                    }
                }
            } else {
                val key = ByteArray(size = KEY_SIZE.toInt()).apply {
                    arc4random_buf(__buf = refTo(0), __nbytes = KEY_SIZE.toULong())
                }
                val cfData = key.usePinned { CFDataCreate(allocator = null, bytes = it.addressOf(index = 0).reinterpret(), length = KEY_SIZE.toLong()) }
                val addQuery = CFDictionaryCreateMutable(allocator = null, capacity = 3, keyCallBacks = null, valueCallBacks = null)
                CFDictionaryAddValue(theDict = addQuery, key = kSecClass, value = kSecClassGenericPassword)
                CFDictionaryAddValue(theDict = addQuery, key = kSecAttrAccount, value = cfAccount)
                CFDictionaryAddValue(theDict = addQuery, key = kSecValueData, value = cfData)
                SecItemAdd(attributes = addQuery, result = null)
                key
            }
        }
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
                data = inputData.refTo(index = 0),
                len = inputData.size.toUInt(),
                md = hash.refTo(index = 0)
            )
            hash.joinToString(separator = "") { it.toString(radix = 16).padStart(length = 2, padChar = '0') }
        }
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to hash", throwable = it)
    }.getOrNull() ?: if (developmentMode) return@withContext content else null
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