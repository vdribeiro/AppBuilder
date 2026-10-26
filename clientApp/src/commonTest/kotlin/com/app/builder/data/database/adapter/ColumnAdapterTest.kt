package com.app.builder.data.database.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.time.Instant
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.test.TestCase

class ColumnAdapterTest: TestCase() {

    /** Verifies that instants are stored as ISO-8601 text and decoded back to the same instant. */
    @Test
    fun instantColumnAdapter() = runUnitTest {
        val instant = now()
        assertEquals(expected = instant, actual = InstantColumnAdapter.decode(databaseValue = InstantColumnAdapter.encode(value = instant)))

        val fixed = Instant.parse(input = "2024-01-02T03:04:05.678Z")
        assertEquals(expected = "2024-01-02T03:04:05.678Z", actual = InstantColumnAdapter.encode(value = fixed))
        assertEquals(expected = fixed, actual = InstantColumnAdapter.decode(databaseValue = "2024-01-02T03:04:05.678Z"))
    }

    /** Verifies that uuids are stored as text, decoded back to the same uuid, and that malformed text is rejected. */
    @Test
    fun uuidColumnAdapter() = runUnitTest {
        val uuid = uuid()
        assertEquals(expected = uuid.toString(), actual = UuidColumnAdapter.encode(value = uuid))
        assertEquals(expected = uuid, actual = UuidColumnAdapter.decode(databaseValue = uuid.toString()))

        assertFails { UuidColumnAdapter.decode(databaseValue = "invalid") }
    }
}
