package com.app.builder.domain.gateway.material

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.core.locale.now
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that a material survives a round trip through its database schema, deletion timestamp included. */
    @Test
    fun materialRoundTrip() = runUnitTest {
        assertEquals(expected = FakeData.material, actual = FakeData.material.toMaterialSchema().toMaterial())

        val deleted = FakeData.material.copy(deletedAt = now())
        assertEquals(expected = deleted, actual = deleted.toMaterialSchema().toMaterial())
    }
}
