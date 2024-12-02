package com.app.builder.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class CryptographyTest: TestCase() {

    /** Verifies that data encrypted with [encrypt] can be decrypted back to the original value. */
    @Test
    fun encryptDecrypt() = runUnitTest {
        assertEquals(
            expected = FakeData.translations,
            actual = decode(value = decrypt(content = encrypt(content = encode(value = FakeData.translations).orEmpty()).orEmpty()).orEmpty())
        )
    }

    /** Verifies that encrypting and decrypting an empty string round trips to an empty string. */
    @Test
    fun encryptDecryptEmptyString() = runUnitTest {
        assertEquals(expected = "", actual = decrypt(content = encrypt(content = "").orEmpty()).orEmpty())
    }

    /**
     * Verifies that [decrypt] fails gracefully on malformed, non-encrypted input instead of throwing.
     * In this development build, the failure path falls back to echoing the original content back unchanged; only in a release build does it resolve to null.
     */
    @Test
    fun decryptMalformedContentFailsGracefully() = runUnitTest {
        val malformed = "not-a-valid-encrypted-payload"
        assertEquals(expected = malformed, actual = decrypt(content = malformed))
    }

    /** Verifies that [hash] produces a non-null result for the given content. */
    @Test
    fun hashing() = runUnitTest {
        assertNotNull(actual = hash(content = "hash"))
    }

    /** Verifies that [hash] is deterministic for the same content and distinct for different content. */
    @Test
    fun hashingIsDeterministicAndDistinct() = runUnitTest {
        val first = hash(content = "hash-content")
        val second = hash(content = "hash-content")
        val different = hash(content = "other-content")

        assertEquals(expected = first, actual = second)
        assertNotEquals(illegal = first, actual = different)
    }
}
