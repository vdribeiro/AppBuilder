package com.app.builder.test

import javax.crypto.SecretKey
import com.app.builder.core.security.KeyStorage
import com.app.builder.core.security.keyStorage

actual fun useUnavailableSecretKey(): Boolean {
    keyStorage = object: KeyStorage {
        override fun read(alias: String): SecretKey? = error(message = "Secret key unavailable")
        override fun generate(alias: String): SecretKey = error(message = "Secret key unavailable")
    }
    return true
}
