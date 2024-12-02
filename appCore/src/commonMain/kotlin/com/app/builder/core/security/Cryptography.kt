package com.app.builder.core.security

/**
 * Encrypts the provided plain text.
 *
 * @param content The plain text to encrypt.
 * @return A Base64 encoded encrypted string, or null if encryption fails.
 */
expect suspend fun encrypt(content: String): String?

/**
 * Decrypts the provided Base64 encoded encrypted text.
 *
 * @param content The Base64 encoded string to decrypt.
 * @return The decrypted plain text, or null if decryption fails.
 */
expect suspend fun decrypt(content: String): String?

/**
 * Generates a cryptographic hash of the provided content.
 *
 * @param content The data to hash.
 * @return The generated hash string, or null if hashing fails.
 */
expect suspend fun hash(content: String): String?
