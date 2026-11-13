package com.app.builder.test

import javax.crypto.spec.SecretKeySpec
import com.app.builder.core.security.KeyStorage
import com.app.builder.core.security.keyStorage

actual fun useUnavailableSecretKey(): Boolean {
    keyStorage = object: KeyStorage {
        override suspend fun load(alias: String): SecretKeySpec = error(message = "Secret key unavailable")
    }
    return true
}
