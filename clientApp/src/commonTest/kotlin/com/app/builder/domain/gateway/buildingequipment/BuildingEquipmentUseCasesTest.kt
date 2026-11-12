package com.app.builder.domain.gateway.buildingequipment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.gateway.UseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingEquipmentUseCasesTest: TestCase() {

    /**
     * Gets the admin's use cases with [FakeData.building] and [FakeData.equipment] stored.
     *
     * @return The admin's [UseCases].
     */
    private suspend fun storedUseCases(): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.equipmentUseCases.upsertEquipment(equipment = FakeData.equipment)
        return useCases
    }

    /** Verifies that a building equipment makes the building and the equipment observable from each other, and deleting it hides both. */
    @Test
    fun linkAndUnlink() = runUnitTest {
        val useCases = storedUseCases()
        val buildingEquipmentUseCases = useCases.buildingEquipmentUseCases

        assertTrue(actual = buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull().orEmpty().isEmpty())
        buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        assertEquals(expected = listOf(element = FakeData.equipment), actual = buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
        assertEquals(expected = listOf(element = FakeData.building), actual = buildingEquipmentUseCases.observeBuildingsOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull())

        buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment.copy(modifiedAt = now(), deletedAt = now()))
        assertTrue(actual = buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = buildingEquipmentUseCases.observeBuildingsOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that deleted equipments and buildings are left out even while they are still linked. */
    @Test
    fun deletedEntitiesAreLeftOut() = runUnitTest {
        val useCases = storedUseCases()
        val buildingEquipmentUseCases = useCases.buildingEquipmentUseCases
        val deletedEquipment = FakeData.equipment.copy(uuid = uuid(), deletedAt = now())
        useCases.equipmentUseCases.upsertEquipment(equipment = deletedEquipment)

        buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment.copy(equipmentUuid = deletedEquipment.uuid))
        assertEquals(expected = listOf(element = FakeData.equipment), actual = buildingEquipmentUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())

        useCases.buildingUseCases.upsertBuilding(building = FakeData.building.copy(deletedAt = now()))
        assertTrue(actual = buildingEquipmentUseCases.observeBuildingsOfEquipment(equipmentUuid = FakeData.equipment.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that an account with building equipment read permission sees building equipments, and cannot change them. */
    @Test
    fun readOnlyAccountCannotLink() = runUnitTest {
        storedUseCases().buildingEquipmentUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment)
        val readerUseCases = dependency.get()
            .getUserDependency(account = FakeData.account)
            .useCases
            .buildingEquipmentUseCases

        assertEquals(expected = listOf(element = FakeData.equipment), actual = readerUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
        readerUseCases.upsertBuildingEquipment(buildingEquipment = FakeData.buildingEquipment.copy(modifiedAt = now(), deletedAt = now()))
        assertEquals(expected = listOf(element = FakeData.equipment), actual = readerUseCases.observeEquipmentsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }
}
