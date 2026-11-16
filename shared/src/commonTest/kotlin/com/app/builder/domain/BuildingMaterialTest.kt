package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import com.app.builder.core.security.uuid
import com.app.builder.core.security.uuidOf
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingMaterialTest: TestCase() {

    /** Verifies a building material's uuid is derived from its building and material, whatever its timestamps and quantity, and differs for another pair. */
    @Test
    fun uuidIsDerivedFromThePair() = runUnitTest {
        val link = FakeData.buildingMaterial
        assertEquals(expected = uuidOf(link.buildingUuid, link.materialUuid), actual = link.uuid)
        assertEquals(expected = link.uuid, actual = link.copy(deletedAt = link.modifiedAt, quantity = link.quantity + 1).uuid)
        assertNotEquals(illegal = link.uuid, actual = link.copy(materialUuid = uuid()).uuid)
    }

    /** Verifies a building material survives a JSON round trip without its uuid in the JSON, and derives the same uuid after decoding. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val json = assertNotNull(actual = encode(value = FakeData.buildingMaterial))
        assertFalse(actual = "\"uuid\"" in json)

        val decoded = assertNotNull(actual = decode<BuildingMaterial>(value = json))
        assertEquals(expected = FakeData.buildingMaterial, actual = decoded)
        assertEquals(expected = FakeData.buildingMaterial.uuid, actual = decoded.uuid)
    }
}
