package com.app.builder.test

import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/**
 * In-memory stand-in for the `AndroidKeyStore` provider, which Robolectric does not supply.
 * It only stores and generates AES secret keys; encryption itself runs on the JVM's own cipher implementations.
 */
object FakeAndroidKeyStore {

    private const val NAME = "AndroidKeyStore"

    /** Replaces any previously registered provider with a fresh one, so every test starts with an empty key store. */
    fun install() {
        Security.removeProvider(NAME)
        Security.addProvider(FakeProvider())
    }

    /** Provider exposing the `AndroidKeyStore` key store and its AES key generator, both backed by the same key map. */
    private class FakeProvider: Provider(NAME, 1.0, "Fake AndroidKeyStore for Robolectric tests") {

        private val secretKeys = Collections.synchronizedMap(mutableMapOf<String, SecretKey>())

        init {
            putService(object: Service(this, "KeyStore", NAME, FakeKeyStoreSpi::class.java.name, null, null) {
                override fun newInstance(constructorParameter: Any?): Any = FakeKeyStoreSpi(keys = secretKeys)
            })
            putService(object: Service(this, "KeyGenerator", KeyProperties.KEY_ALGORITHM_AES, FakeKeyGeneratorSpi::class.java.name, null, null) {
                override fun newInstance(constructorParameter: Any?): Any = FakeKeyGeneratorSpi(keys = secretKeys)
            })
        }
    }

    /** Key store holding secret keys by alias; certificates are not supported. */
    private class FakeKeyStoreSpi(private val keys: MutableMap<String, SecretKey>): KeyStoreSpi() {

        override fun engineGetKey(alias: String, password: CharArray?): Key? = keys[alias]

        override fun engineSetKeyEntry(alias: String, key: Key, password: CharArray?, chain: Array<out Certificate>?) {
            keys[alias] = key as SecretKey
        }

        override fun engineSetKeyEntry(alias: String, key: ByteArray, chain: Array<out Certificate>?) = throw UnsupportedOperationException()

        override fun engineSetCertificateEntry(alias: String, cert: Certificate) = throw UnsupportedOperationException()

        override fun engineDeleteEntry(alias: String) {
            keys.remove(alias)
        }

        override fun engineGetCertificateChain(alias: String): Array<Certificate>? = null

        override fun engineGetCertificate(alias: String): Certificate? = null

        override fun engineGetCreationDate(alias: String): Date? = null

        override fun engineAliases(): Enumeration<String> = Collections.enumeration(keys.keys.toList())

        override fun engineContainsAlias(alias: String): Boolean = keys.containsKey(alias)

        override fun engineSize(): Int = keys.size

        override fun engineIsKeyEntry(alias: String): Boolean = keys.containsKey(alias)

        override fun engineIsCertificateEntry(alias: String): Boolean = false

        override fun engineGetCertificateAlias(cert: Certificate): String? = null

        override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit

        override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
    }

    /** AES key generator that, like the real provider, stores each generated key under the alias of its [KeyGenParameterSpec]. */
    private class FakeKeyGeneratorSpi(private val keys: MutableMap<String, SecretKey>): KeyGeneratorSpi() {

        private var spec: KeyGenParameterSpec? = null
        private var random: SecureRandom = SecureRandom()

        override fun engineInit(params: AlgorithmParameterSpec, random: SecureRandom?) {
            spec = params as? KeyGenParameterSpec ?: throw IllegalArgumentException("Expected KeyGenParameterSpec, got $params")
            random?.let { this.random = it }
        }

        override fun engineInit(random: SecureRandom?) = throw UnsupportedOperationException("A KeyGenParameterSpec is required")

        override fun engineInit(keysize: Int, random: SecureRandom?) = throw UnsupportedOperationException("A KeyGenParameterSpec is required")

        override fun engineGenerateKey(): SecretKey {
            val spec = checkNotNull(spec) { "KeyGenerator not initialized" }
            val bytes = ByteArray(size = spec.keySize / Byte.SIZE_BITS).also { random.nextBytes(it) }
            return SecretKeySpec(bytes, KeyProperties.KEY_ALGORITHM_AES).also { keys[spec.keystoreAlias] = it }
        }
    }
}
