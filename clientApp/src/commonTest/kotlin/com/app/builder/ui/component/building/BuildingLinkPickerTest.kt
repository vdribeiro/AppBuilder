package com.app.builder.ui.component.building

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.link.LinkPickerAction

class BuildingLinkPickerTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true) }
    }

    /** Verifies that the picker applies the building list filters, searching and leaving out the deleted buildings, and ticks the picked buildings. */
    @Test
    fun pickerAppliesBuildingFilters() = runUnitTest {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val other = FakeData.building.copy(uuid = uuid(), name = "Pump")
        val deleted = FakeData.building.copy(uuid = uuid(), name = "Pump gone", deletedAt = now())
        listOf(FakeData.building, other, deleted).forEach { useCases.buildingUseCases.upsertBuilding(building = it) }
        AppFile.BuildingPreferences.save { buildingFilterDefaults.copy(search = "pump", showDeleted = true) }
        val store = buildingLinkPickerStore(buildingUseCases = useCases.buildingUseCases)

        val state = store.stateFlow.first { it.candidates.size == 2 && it.filters.search == "pump" }
        assertEquals(expected = listOf(other.uuid.toString()), actual = state.items.map { it.uuid })
        assertEquals(expected = listOf<Boolean?>(false), actual = state.items.map { it.checked })

        store.send(action = LinkPickerAction.Open(picked = setOf(other.uuid)))
        assertEquals(expected = listOf<Boolean?>(true), actual = store.state.items.map { it.checked })
        assertEquals(expected = listOf(element = other), actual = store.state.pickedCandidates)
    }
}
