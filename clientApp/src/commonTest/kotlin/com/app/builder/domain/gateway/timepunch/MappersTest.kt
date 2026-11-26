package com.app.builder.domain.gateway.timepunch

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.core.locale.now
import com.app.builder.domain.TimePunch
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that a time punch survives a round trip through its database schema, deletion timestamp and source included. */
    @Test
    fun timePunchRoundTrip() = runUnitTest {
        assertEquals(expected = FakeData.timePunch, actual = FakeData.timePunch.toTimePunchSchema().toTimePunch())

        val deleted = FakeData.timePunch.copy(deletedAt = now(), source = TimePunch.Source.entries.last())
        assertEquals(expected = deleted, actual = deleted.toTimePunchSchema().toTimePunch())
    }
}
