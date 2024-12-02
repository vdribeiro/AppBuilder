package com.app.builder.core.security

import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import org.springframework.security.crypto.bcrypt.BCrypt

/**
 * Hashes a plaintext password securely.
 *
 * @param password The plaintext password to be hashed.
 * @return The securely hashed string containing the mathematically embedded random salt, or null if an error occurs.
 */
suspend fun hashPassword(password: String): String? = withContext(context = Dispatcher.Default) {
    runCatching {
        BCrypt.hashpw(password, BCrypt.gensalt())
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to hash password", throwable = it)
    }.getOrNull()
}

/**
 * Verifies a plaintext password against a securely stored hash.
 *
 * @param password The plaintext password provided during authentication.
 * @param hash The securely stored hash retrieved from the database to check against.
 * @return `true` if the password matches the hash, `false` otherwise or if the hash is malformed.
 */
suspend fun verifyPassword(password: String, hash: String): Boolean = withContext(context = Dispatcher.Default) {
    runCatching {
        BCrypt.checkpw(password, hash)
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to verify password hash", throwable = it)
    }.getOrDefault(defaultValue = false)
}

private const val TAG = "Cryptography"
