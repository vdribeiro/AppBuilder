package com.app.builder.domain.gateway.building

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class BuildingUseCasesTest: TestCase() {

    /** Verifies that upserting a building makes it observable both individually and in the building list. */
    @Test
    fun crudBuilding() = runUnitTest {
        val buildingUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .buildingUseCases

        assertTrue(actual = buildingUseCases.observeBuildings().firstOrNull().orEmpty().isEmpty())
        buildingUseCases.upsertBuilding(building = FakeData.building)
        assertEquals(expected = listOf(element = FakeData.building), actual = buildingUseCases.observeBuildings().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.building, actual = buildingUseCases.observeBuilding(uuid = FakeData.building.uuid).firstOrNull())
    }
}
