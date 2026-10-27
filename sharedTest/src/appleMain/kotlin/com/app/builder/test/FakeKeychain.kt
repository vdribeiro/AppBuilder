package com.app.builder.test

import com.app.builder.core.security.KeyStorage
import com.app.builder.core.security.keyStorage

/**
 * In-memory stand-in for the iOS Keychain, which is not available to tests running on the simulator.
 * It only stores the secret key; encryption itself runs on the platform's own CommonCrypto implementation.
 */
object FakeKeychain {

    /** Replaces the key storage with a fresh, empty one, so every test starts without a key. */
    fun install() {
        keyStorage = FakeKeyStorage()
    }

    /** Key storage holding keys by account, rejecting a second key for the same account like the Keychain does. */
    private class FakeKeyStorage: KeyStorage {

        private val keys = mutableMapOf<String, ByteArray>()

        override fun read(account: String): ByteArray? = keys[account]?.copyOf()

        override fun add(account: String, key: ByteArray) {
            check(value = account !in keys) { "Duplicate item for $account" }
            keys[account] = key.copyOf()
        }
    }
}
