package com.app.builder.test

import com.app.builder.core.security.KeyStorage
import com.app.builder.core.security.keyStorage

actual fun useUnavailableSecretKey(): Boolean {
    keyStorage = object: KeyStorage {
        override fun read(account: String): ByteArray? = error(message = "Secret key unavailable")
        override fun add(account: String, key: ByteArray) = error(message = "Secret key unavailable")
    }
    return true
}
