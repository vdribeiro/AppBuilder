package com.app.builder.test

import java.util.Collections
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import com.app.builder.core.security.KeyStorage
import com.app.builder.core.security.keyStorage

/**
 * In-memory stand-in for the Android Keystore, which is not available to tests running on the host.
 * It only stores and generates AES secret keys; encryption itself runs on the JVM's own cipher implementations.
 */
object FakeAndroidKeyStore {

    /** Replaces the key storage with a fresh, empty one, so every test starts without a key. */
    fun install() {
        keyStorage = FakeKeyStorage()
    }

    /** Key storage holding generated AES keys by alias. */
    private class FakeKeyStorage: KeyStorage {

        private val keys = Collections.synchronizedMap(mutableMapOf<String, SecretKey>())

        override fun read(alias: String): SecretKey? = keys[alias]

        override fun generate(alias: String): SecretKey = KeyGenerator.getInstance(ALGORITHM).apply { init(KEY_SIZE) }.generateKey().also { keys[alias] = it }
    }

    private const val ALGORITHM = "AES"
    private const val KEY_SIZE = 256
}
