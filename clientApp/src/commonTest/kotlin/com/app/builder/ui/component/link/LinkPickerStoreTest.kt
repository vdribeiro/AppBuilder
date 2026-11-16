package com.app.builder.ui.component.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.AppFile
import com.app.builder.test.TestCase

class LinkPickerStoreTest: TestCase() {

    /**
     * An entity to pick.
     *
     * @property uuid The unique identifier of the entity.
     * @property name The name of the entity.
     */
    private data class Candidate(val uuid: Uuid = uuid(), val name: String = "")

    private val first = Candidate(name = "First")
    private val second = Candidate(name = "Second")
    private val candidates = MutableStateFlow(value = listOf(first, second))
    private val filters = MutableStateFlow(value = AppFile.ActionBarData())

    /**
     * Creates a picker store over [candidates] and [filters], whose rows are the names of the candidates containing the search, ticked ones marked with a star.
     *
     * @return The [LinkPickerStore] instance.
     */
    private fun pickerStore(): LinkPickerStore<Candidate, String> = LinkPickerStore(
        state = LinkPickerState(),
        candidates = candidates,
        filters = filters,
        uuid = { it.uuid },
        toItems = { candidates, filters, picked ->
            candidates.filter { filters.search in it.name }.map { if (it.uuid in picked) "${it.name}*" else it.name }
        }
    )

    /** Verifies that the picker opens with the given entities ticked, ticks and unticks an entity, and clears the ticks when closed. */
    @Test
    fun openToggleClose() = runUnitTest {
        val store = pickerStore()
        store.stateFlow.first { it.candidates.size == 2 }

        store.send(action = LinkPickerAction.Open(picked = setOf(first.uuid)))
        assertTrue(actual = store.state.open)
        assertEquals(expected = listOf("First*", "Second"), actual = store.state.items)
        assertEquals(expected = listOf(element = first), actual = store.state.pickedCandidates)

        store.send(action = LinkPickerAction.Toggle(uuid = second.uuid))
        store.send(action = LinkPickerAction.Toggle(uuid = first.uuid))
        assertEquals(expected = listOf("First", "Second*"), actual = store.state.items)
        assertEquals(expected = listOf(element = second), actual = store.state.pickedCandidates)

        store.send(action = LinkPickerAction.Close)
        assertFalse(actual = store.state.open)
        assertTrue(actual = store.state.picked.isEmpty())
        assertTrue(actual = store.state.pickedCandidates.isEmpty())
        assertEquals(expected = listOf("First", "Second"), actual = store.state.items)
    }

    /** Verifies that the rows follow the entities and the list filters, while a ticked entity hidden by the filters stays ticked. */
    @Test
    fun followsCandidatesAndFilters() = runUnitTest {
        val store = pickerStore()
        store.stateFlow.first { it.candidates.size == 2 }
        store.send(action = LinkPickerAction.Open(picked = setOf(first.uuid)))

        filters.value = AppFile.ActionBarData(search = "Sec")
        assertEquals(expected = listOf(element = "Second"), actual = store.stateFlow.first { it.filters.search == "Sec" }.items)
        assertEquals(expected = listOf(element = first), actual = store.state.pickedCandidates)

        candidates.value = listOf(second)
        val state = store.stateFlow.first { it.candidates.size == 1 }
        assertEquals(expected = listOf(element = "Second"), actual = state.items)
        assertTrue(actual = state.pickedCandidates.isEmpty())
    }
}
