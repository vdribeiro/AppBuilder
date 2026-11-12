package com.app.builder.test

import java.util.Collections
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import com.app.builder.core.security.KeyStorage
import com.app.builder.core.security.keyStorage

/**
 * In-memory stand-in for the desktop keystore file and the operating system's secret store that unlocks it.
 * It only stores and generates AES secret keys; encryption itself runs on the JVM's own cipher implementations.
 */
object FakeDesktopKeyStore {

    /** Replaces the key storage with a fresh, empty one, so every test starts without a key. */
    fun install() {
        keyStorage = FakeKeyStorage()
    }

    /** Key storage holding AES keys by alias, generating the key of an alias the first time it is loaded. */
    private class FakeKeyStorage: KeyStorage {

        private val keys = Collections.synchronizedMap(mutableMapOf<String, SecretKeySpec>())

        override suspend fun load(alias: String): SecretKeySpec = keys.getOrPut(key = alias) {
            SecretKeySpec(KeyGenerator.getInstance(ALGORITHM).apply { init(KEY_SIZE) }.generateKey().encoded, ALGORITHM)
        }
    }

    private const val ALGORITHM = "AES"
    private const val KEY_SIZE = 256
}
