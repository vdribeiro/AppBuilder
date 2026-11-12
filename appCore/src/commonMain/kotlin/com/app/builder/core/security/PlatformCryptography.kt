package com.app.builder.core.security

/**
 * Encrypts [content] with the platform secret key.
 *
 * @param content The plain text to encrypt.
 * @return The Base64 encoded encrypted string, or throw on error.
 */
internal expect suspend fun encryptContent(content: String): String

/**
 * Decrypts [content] with the platform secret key.
 *
 * @param content The Base64 encoded string to decrypt.
 * @return The decrypted plain text, or throw on error.
 */
internal expect suspend fun decryptContent(content: String): String

/**
 * Computes the SHA-256 digest of [content].
 *
 * @param content The data to hash.
 * @return The digest bytes, or throw on error.
 */
internal expect suspend fun sha256(content: String): ByteArray

/**
 * Derives a key from [password] with PBKDF2-HMAC-SHA256.
 *
 * @param password The password to derive from.
 * @param salt The per-entry salt.
 * @param rounds The number of PBKDF2 rounds.
 * @param size The size, in bits, of the derived key.
 * @return The derived key bytes, or throw on error.
 */
internal expect suspend fun pbkdf2(password: String, salt: String, rounds: Int, size: Int): ByteArray
