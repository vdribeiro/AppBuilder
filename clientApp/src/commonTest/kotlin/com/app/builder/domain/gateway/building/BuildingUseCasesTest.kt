package com.app.builder.domain.gateway.building

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingUseCasesTest: TestCase() {

    /** Verifies that upserting a building makes it observable both individually and in the building list. */
    @Test
    fun crudBuilding() = runUnitTest {
        val buildingUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .buildingUseCases

        assertTrue(actual = buildingUseCases.observeBuildings().firstOrNull().orEmpty().isEmpty())
        buildingUseCases.upsertBuilding(building = FakeData.building)
        assertEquals(expected = listOf(element = FakeData.building), actual = buildingUseCases.observeBuildings().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.building, actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())
    }

    /** Verifies that the building list includes the deleted buildings. */
    @Test
    fun filterBuildings() = runUnitTest {
        val buildingUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .buildingUseCases
        val deletedBuilding = FakeData.building.copy(uuid = uuid(), deletedAt = now())

        buildingUseCases.upsertBuilding(building = FakeData.building)
        buildingUseCases.upsertBuilding(building = deletedBuilding)

        assertEquals(expected = setOf(FakeData.building, deletedBuilding), actual = buildingUseCases.observeBuildings().firstOrNull().orEmpty().toSet())
    }
}
