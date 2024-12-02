package com.app.builder.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import com.app.builder.test.TestCase

class UuidTest: TestCase() {

    /** Pattern matching the canonical lowercase hyphenated UUID string format. */
    private val uuidRegex = Regex(pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

    /**
     * Asserts the given UUID has a valid canonical string form and round-trips through toUuid.
     *
     * @param uuid The UUID to validate.
     * @return The same [uuid], for chaining into further assertions.
     */
    private fun testUuid(uuid: Uuid): Uuid {
        val uuidString = uuid.toString()
        assertEquals(expected = 36, actual = uuidString.length)
        assertTrue(actual = uuidString.matches(regex = uuidRegex))
        assertEquals(expected = uuid, actual = uuidString.toUuid())
        return uuid
    }

    /** Verifies uuid generates distinct, well-formed UUIDs on each call. */
    @Test
    fun generateUuid() = runUnitTest {
        val uuid = testUuid(uuid = uuid())
        assertNotEquals(illegal = uuid, actual = testUuid(uuid = uuid()))
    }

    /** Verifies uuidV7 generates distinct, well-formed version-7 UUIDs. */
    @Test
    fun generateUuidV7() = runUnitTest {
        val uuid = testUuid(uuid = uuidV7()!!)
        val uuidString = uuid.toString()
        assertEquals(expected = '7', actual = uuidString[14])
        assertNotEquals(illegal = uuid, actual = testUuid(uuid = uuidV7()!!))
    }

    /** Verifies uuidV4 generates distinct, well-formed version-4 UUIDs. */
    @Test
    fun generateUuidV4() = runUnitTest {
        val uuid = testUuid(uuid = uuidV4()!!)
        val uuidString = uuid.toString()
        assertEquals(expected = '4', actual = uuidString[14])
        assertNotEquals(illegal = uuid, actual = testUuid(uuid = uuidV4()!!))
    }

    /** Verifies unsecureUuid generates distinct, well-formed UUIDs with a valid variant bit. */
    @Test
    fun generateUnsecureUuid() = runUnitTest {
        val uuid = testUuid(uuid = unsecureUuid())
        val uuidString = uuid.toString()
        assertEquals(expected = '7', actual = uuidString[14])
        assertTrue(actual = uuidString[19] in listOf('8', '9', 'a', 'b'))
        assertNotEquals(illegal = uuid, actual = testUuid(uuid = unsecureUuid()))
    }

    /** Verifies toUuid returns null when parsing a malformed UUID string. */
    @Test
    fun toUuidReturnsNullForInvalidString() = runUnitTest {
        assertNull(actual = "not-a-uuid".toUuid())
    }
}
