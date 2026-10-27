package com.app.builder.core.security

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy
import com.app.builder.test.ExcludeFromTesting
import com.app.builder.toCFString
import com.app.builder.use

/** Where the secret key used to encrypt and decrypt app data is kept. */
interface KeyStorage {

    /**
     * Reads the key stored under [account].
     *
     * @param account The account the key is filed under.
     * @return The key, or null when none is stored.
     * @throws IllegalStateException When the storage cannot be read, so an unreadable key is never mistaken for a missing one.
     */
    fun read(account: String): ByteArray?

    /**
     * Stores [key] under [account], which must not hold a key yet.
     *
     * @param account The account the key is filed under.
     * @param key The key to store.
     * @throws IllegalStateException When the key cannot be stored, including when [account] already holds one.
     */
    fun add(account: String, key: ByteArray)
}

/** The key storage the app encrypts and decrypts with. */
var keyStorage: KeyStorage = KeychainKeyStorage

/** The iOS Keychain, holding the key as a generic password item. */
@ExcludeFromTesting
internal object KeychainKeyStorage: KeyStorage {

    override fun read(account: String): ByteArray? = memScoped {
        account.toCFString().use { cfAccount ->
            CFDictionaryCreateMutable(allocator = null, capacity = 4, keyCallBacks = null, valueCallBacks = null).use { query ->
                CFDictionaryAddValue(theDict = query, key = kSecClass, value = kSecClassGenericPassword)
                CFDictionaryAddValue(theDict = query, key = kSecAttrAccount, value = cfAccount)
                CFDictionaryAddValue(theDict = query, key = kSecReturnData, value = kCFBooleanTrue)
                CFDictionaryAddValue(theDict = query, key = kSecMatchLimit, value = kSecMatchLimitOne)
                val result = alloc<CFTypeRefVar>()
                when (val status = SecItemCopyMatching(query = query, result = result.ptr)) {
                    errSecSuccess -> result.value.use { value ->
                        @Suppress("UNCHECKED_CAST") val data = value as CFDataRef
                        val bytes = CFDataGetBytePtr(theData = data)
                        val length = CFDataGetLength(theData = data)
                        ByteArray(size = length.toInt()).apply {
                            if (isNotEmpty()) usePinned { pinned -> memcpy(__dst = pinned.addressOf(0), __src = bytes, __n = length.toULong()) }
                        }
                    }

                    errSecItemNotFound -> null
                    else -> error(message = "Unable to read secret key: $status")
                }
            }
        }
    }

    override fun add(account: String, key: ByteArray) {
        val data = key.usePinned { CFDataCreate(allocator = null, bytes = it.addressOf(index = 0).reinterpret(), length = key.size.toLong()) }
        data.use {
            account.toCFString().use { cfAccount ->
                CFDictionaryCreateMutable(allocator = null, capacity = 3, keyCallBacks = null, valueCallBacks = null).use { query ->
                    CFDictionaryAddValue(theDict = query, key = kSecClass, value = kSecClassGenericPassword)
                    CFDictionaryAddValue(theDict = query, key = kSecAttrAccount, value = cfAccount)
                    CFDictionaryAddValue(theDict = query, key = kSecValueData, value = data)
                    val status = SecItemAdd(attributes = query, result = null)
                    if (status != errSecSuccess) error(message = "Unable to persist secret key: $status")
                }
            }
        }
    }
}
