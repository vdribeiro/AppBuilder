package com.app.builder.core.security

import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry

/**
 * Encrypts the provided plain text.
 *
 * @param content The plain text to encrypt.
 * @return A Base64 encoded encrypted string, or null if encryption fails.
 */
suspend fun encrypt(content: String): String? = runCryptography(operation = "encrypt") { encryptContent(content = content) }

/**
 * Decrypts the provided Base64 encoded encrypted text.
 *
 * @param content The Base64 encoded string to decrypt.
 * @return The decrypted plain text, or null if decryption fails.
 */
suspend fun decrypt(content: String): String? = runCryptography(operation = "decrypt") { decryptContent(content = content) }

/**
 * Generates a cryptographic hash of the provided content.
 *
 * @param content The data to hash.
 * @return The generated hash as a lowercase hex string, or null if hashing fails.
 */
suspend fun hash(content: String): String? = runCryptography(operation = "hash") { sha256(content = content).toHexString() }

/**
 * Derives a key from a password using PBKDF2-HMAC-SHA256.
 *
 * @param password The password to derive from.
 * @param salt The per-entry salt, which must not be empty.
 * @return The derived key as a lowercase hex string, or null if [salt] is empty or derivation fails.
 */
suspend fun deriveKey(password: String, salt: String): String? = runCryptography(operation = "derive key") {
    require(value = salt.isNotEmpty()) { "Salt must not be empty" }
    pbkdf2(password = password, salt = salt, rounds = DERIVATION_ROUNDS, size = DERIVED_KEY_SIZE).toHexString()
}

/**
 * Runs [block] on [Dispatcher.Default] and reports a failure to [Telemetry].
 *
 * @param operation The name of the operation, used in the failure message.
 * @param block The cryptographic operation to run.
 * @return The result of [block], or null if it throws.
 */
private suspend fun <T> runCryptography(operation: String, block: suspend () -> T): T? = withContext(context = Dispatcher.Default) {
    runCatching {
        block()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to $operation", throwable = it)
    }.getOrNull()
}

private const val TAG = "Cryptography"

/** The number of PBKDF2 rounds. */
private const val DERIVATION_ROUNDS = 210_000
/** The size, in bits, of the derived key. */
private const val DERIVED_KEY_SIZE = 256
