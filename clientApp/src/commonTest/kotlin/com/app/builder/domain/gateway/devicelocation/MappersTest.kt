package com.app.builder.domain.gateway.devicelocation

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that a device location survives a round trip through its database schema, with and without its optional readings. */
    @Test
    fun deviceLocationRoundTrip() = runUnitTest {
        assertEquals(expected = FakeData.deviceLocation, actual = FakeData.deviceLocation.toDeviceLocationSchema().toDeviceLocation())

        val partial = FakeData.deviceLocation.copy(altitude = null, bearing = null, speed = null)
        assertEquals(expected = partial, actual = partial.toDeviceLocationSchema().toDeviceLocation())
    }
}
