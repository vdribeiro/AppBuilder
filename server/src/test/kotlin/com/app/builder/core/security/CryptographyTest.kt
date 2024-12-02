package com.app.builder.core.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.app.builder.test.TestCase

class CryptographyTest: TestCase() {

    /** Verifies that hashing a password returns a non-null hash. */
    @Test
    fun hashPasswordReturnsNonNullHash() = runServerTest {
        assertNotNull(actual = hashPassword("password123"))
    }

    /** Verifies that hashing the same password twice produces different hashes. */
    @Test
    fun hashPasswordProducesDifferentHashesForSamePassword() = runServerTest {
        val hash1 = hashPassword(password = "password123")
        val hash2 = hashPassword(password = "password123")
        assertNotNull(actual = hash1)
        assertNotNull(actual = hash2)
        assertNotEquals(illegal = hash1, actual = hash2)
    }

    /** Verifies that verifying a password against its own hash succeeds. */
    @Test
    fun verifyPasswordReturnsTrueForMatchingHash() = runServerTest {
        val password = "secret123"
        val hash = requireNotNull(value = hashPassword(password))
        assertTrue(actual = verifyPassword(password = password, hash = hash))
    }

    /** Verifies that verifying a wrong password against a hash fails. */
    @Test
    fun verifyPasswordReturnsFalseForWrongPassword() = runServerTest {
        val hash = requireNotNull(value = hashPassword(password = "correct"))
        assertFalse(actual = verifyPassword(password = "wrong", hash = hash))
    }

    /** Verifies that verifying a password against a malformed hash fails instead of throwing. */
    @Test
    fun verifyPasswordReturnsFalseForMalformedHash() = runServerTest {
        assertFalse(actual = verifyPassword(password = "password", hash = "not-a-bcrypt-hash"))
    }
}
