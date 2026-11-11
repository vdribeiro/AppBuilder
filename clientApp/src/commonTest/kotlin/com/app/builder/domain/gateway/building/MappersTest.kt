package com.app.builder.domain.gateway.building

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.core.locale.now
import com.app.builder.domain.Building
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that a building survives a round trip through its database schema, deletion timestamp and missing coordinates included. */
    @Test
    fun buildingRoundTrip() = runUnitTest {
        assertEquals(expected = FakeData.building, actual = FakeData.building.toBuildingSchema().toBuilding())

        val deleted = FakeData.building.copy(deletedAt = now(), latitude = null, longitude = null)
        assertEquals(expected = deleted, actual = deleted.toBuildingSchema().toBuilding())
    }
}
