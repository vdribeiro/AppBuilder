package com.app.builder.ui.component.material

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.link.LinkPickerAction

class MaterialLinkPickerTest: TestCase() {

    /** Verifies that the picker applies the material list filters, searching and leaving out the deleted materials, and ticks the picked materials. */
    @Test
    fun pickerAppliesMaterialFilters() = runUnitTest {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        val other = FakeData.material.copy(uuid = uuid(), name = "Pump")
        val deleted = FakeData.material.copy(uuid = uuid(), name = "Pump gone", deletedAt = now())
        listOf(FakeData.material, other, deleted).forEach { useCases.materialUseCases.upsertMaterial(material = it) }
        AppFile.MaterialPreferences.save { materialFilterDefaults.copy(search = "pump", showDeleted = true) }
        val store = materialLinkPickerStore(materialUseCases = useCases.materialUseCases)

        val state = store.stateFlow.first { it.candidates.size == 2 && it.filters.search == "pump" }
        assertEquals(expected = listOf(other.uuid.toString()), actual = state.items.map { it.uuid })
        assertEquals(expected = listOf<Boolean?>(false), actual = state.items.map { it.checked })

        store.send(action = LinkPickerAction.Open(picked = setOf(other.uuid)))
        assertEquals(expected = listOf<Boolean?>(true), actual = store.state.items.map { it.checked })
        assertEquals(expected = listOf(element = other), actual = store.state.pickedCandidates)
    }
}
