package com.app.builder.domain.gateway.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that a registry survives a round trip through its database schema, operating system included. */
    @Test
    fun registryRoundTrip() = runUnitTest {
        val schema = FakeData.registry.toRegistrySchema()

        assertEquals(expected = FakeData.registry.os.toString(), actual = schema.os)
        assertEquals(expected = FakeData.registry, actual = schema.toRegistry())
    }
}
