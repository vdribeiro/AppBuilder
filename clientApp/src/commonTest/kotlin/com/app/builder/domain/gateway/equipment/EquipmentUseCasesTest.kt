package com.app.builder.domain.gateway.equipment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class EquipmentUseCasesTest: TestCase() {

    /** Verifies that upserting a equipment makes it observable both individually and in the equipment list. */
    @Test
    fun crudEquipment() = runUnitTest {
        val equipmentUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .equipmentUseCases

        assertTrue(actual = equipmentUseCases.observeEquipments().firstOrNull().orEmpty().isEmpty())
        equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        assertEquals(expected = listOf(element = FakeData.equipment), actual = equipmentUseCases.observeEquipments().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.equipment, actual = equipmentUseCases.observeEquipment(uuid = FakeData.equipment.uuid).firstOrNull())
    }
}
