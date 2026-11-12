package com.app.builder.core.security

import javax.crypto.spec.SecretKeySpec

/** Where the secret key used to encrypt and decrypt app data is kept. */
interface KeyStorage {

    /**
     * Loads the key stored under [alias].
     *
     * @param alias The alias the key is stored under.
     * @return The key.
     * @throws IllegalStateException When the key cannot be loaded.
     */
    suspend fun load(alias: String): SecretKeySpec
}

/** The key storage the app encrypts and decrypts with, or null when it is the keystore file in the app data directory. */
var keyStorage: KeyStorage? = null
