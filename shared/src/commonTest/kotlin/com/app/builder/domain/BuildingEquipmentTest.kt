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

class BuildingEquipmentTest: TestCase() {

    /** Verifies a building equipment's uuid is derived from its building and equipment, whatever its timestamps, and differs for another pair. */
    @Test
    fun uuidIsDerivedFromThePair() = runUnitTest {
        val link = FakeData.buildingEquipment
        assertEquals(expected = uuidOf(link.buildingUuid, link.equipmentUuid), actual = link.uuid)
        assertEquals(expected = link.uuid, actual = link.copy(deletedAt = link.modifiedAt).uuid)
        assertNotEquals(illegal = link.uuid, actual = link.copy(equipmentUuid = uuid()).uuid)
    }

    /** Verifies a building equipment survives a JSON round trip without its uuid in the JSON, and derives the same uuid after decoding. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val json = assertNotNull(actual = encode(value = FakeData.buildingEquipment))
        assertFalse(actual = "\"uuid\"" in json)

        val decoded = assertNotNull(actual = decode<BuildingEquipment>(value = json))
        assertEquals(expected = FakeData.buildingEquipment, actual = decoded)
        assertEquals(expected = FakeData.buildingEquipment.uuid, actual = decoded.uuid)
    }
}
