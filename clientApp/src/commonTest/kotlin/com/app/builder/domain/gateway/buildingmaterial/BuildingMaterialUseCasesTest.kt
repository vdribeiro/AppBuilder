package com.app.builder.domain.gateway.buildingmaterial

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

class BuildingMaterialUseCasesTest: TestCase() {

    /** [FakeData.material] as stored in [FakeData.building] by [FakeData.buildingMaterial]. */
    private val storedMaterial = MaterialInBuilding(material = FakeData.material, quantity = FakeData.buildingMaterial.quantity)

    /** [FakeData.building] as storing [FakeData.material] by [FakeData.buildingMaterial]. */
    private val storingBuilding = BuildingOfMaterial(building = FakeData.building, quantity = FakeData.buildingMaterial.quantity)

    /**
     * Gets the admin's use cases with [FakeData.building] and [FakeData.material] stored.
     *
     * @return The admin's [UseCases].
     */
    private suspend fun storedUseCases(): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.buildingUseCases.upsertBuilding(building = FakeData.building)
        useCases.materialUseCases.upsertMaterial(material = FakeData.material)
        return useCases
    }

    /** Verifies that a building material makes the building and the material observable from each other with its quantity, and deleting it hides both. */
    @Test
    fun linkAndUnlink() = runUnitTest {
        val useCases = storedUseCases()
        val buildingMaterialUseCases = useCases.buildingMaterialUseCases

        assertTrue(actual = buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull().orEmpty().isEmpty())
        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        assertEquals(expected = listOf(element = storedMaterial), actual = buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
        assertEquals(expected = listOf(element = storingBuilding), actual = buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = FakeData.material.uuid).firstOrNull())

        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial.copy(modifiedAt = now(), deletedAt = now()))
        assertTrue(actual = buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull().orEmpty().isEmpty())
        assertTrue(actual = buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = FakeData.material.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that upserting a building material again changes the quantity observed from both the building and the material. */
    @Test
    fun changeQuantity() = runUnitTest {
        val buildingMaterialUseCases = storedUseCases().buildingMaterialUseCases
        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)

        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial.copy(modifiedAt = now(), quantity = 7))
        assertEquals(expected = listOf(element = storedMaterial.copy(quantity = 7)), actual = buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
        assertEquals(expected = listOf(element = storingBuilding.copy(quantity = 7)), actual = buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = FakeData.material.uuid).firstOrNull())
    }

    /** Verifies that deleted materials and buildings are left out even while they are still linked. */
    @Test
    fun deletedEntitiesAreLeftOut() = runUnitTest {
        val useCases = storedUseCases()
        val buildingMaterialUseCases = useCases.buildingMaterialUseCases
        val deletedMaterial = FakeData.material.copy(uuid = uuid(), deletedAt = now())
        useCases.materialUseCases.upsertMaterial(material = deletedMaterial)

        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial.copy(materialUuid = deletedMaterial.uuid))
        assertEquals(expected = listOf(element = storedMaterial), actual = buildingMaterialUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())

        useCases.buildingUseCases.upsertBuilding(building = FakeData.building.copy(deletedAt = now()))
        assertTrue(actual = buildingMaterialUseCases.observeBuildingsOfMaterial(materialUuid = FakeData.material.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that an account with building material read permission sees building materials, and cannot change them. */
    @Test
    fun readOnlyAccountCannotLink() = runUnitTest {
        storedUseCases().buildingMaterialUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial)
        val readerUseCases = dependency.get()
            .getUserDependency(account = FakeData.account)
            .useCases
            .buildingMaterialUseCases

        assertEquals(expected = listOf(element = storedMaterial), actual = readerUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
        readerUseCases.upsertBuildingMaterial(buildingMaterial = FakeData.buildingMaterial.copy(modifiedAt = now(), deletedAt = now()))
        assertEquals(expected = listOf(element = storedMaterial), actual = readerUseCases.observeMaterialsInBuilding(buildingUuid = FakeData.building.uuid).firstOrNull())
    }
}
