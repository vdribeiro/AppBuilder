package com.app.builder.core.security

import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.app.builder.test.ExcludeFromTesting

/** Where the secret key used to encrypt and decrypt app data is kept. */
interface KeyStorage {

    /**
     * Reads the key stored under [alias].
     *
     * @param alias The alias the key is stored under.
     * @return The key, or null when none is stored.
     */
    fun read(alias: String): SecretKey?

    /**
     * Generates a key and stores it under [alias].
     *
     * @param alias The alias the key is stored under.
     * @return The generated key.
     */
    fun generate(alias: String): SecretKey
}

/** The key storage the app encrypts and decrypts with. */
var keyStorage: KeyStorage = AndroidKeyStorage

/** The Android Keystore, holding the key as an AES-GCM key that never leaves it. */
@ExcludeFromTesting
internal object AndroidKeyStorage: KeyStorage {

    override fun read(alias: String): SecretKey? = KeyStore.getInstance(PROVIDER).apply { load(null) }.getKey(alias, null) as? SecretKey

    override fun generate(alias: String): SecretKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER).apply { init(spec) }.generateKey()
    }
}

/** The [KeyStore] provider used to store the secret key in the Android Keystore system. */
private const val PROVIDER = "AndroidKeyStore"
/** The size, in bits, of the AES secret key. */
private const val KEY_SIZE = 256
