package com.app.builder.domain.gateway.equipment

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.core.locale.now
import com.app.builder.domain.Equipment
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that an equipment survives a round trip through its database schema, deletion timestamp and status included. */
    @Test
    fun equipmentRoundTrip() = runUnitTest {
        assertEquals(expected = FakeData.equipment, actual = FakeData.equipment.toEquipmentSchema().toEquipment())

        val deleted = FakeData.equipment.copy(deletedAt = now(), status = Equipment.Status.entries.last())
        assertEquals(expected = deleted, actual = deleted.toEquipmentSchema().toEquipment())
    }
}
