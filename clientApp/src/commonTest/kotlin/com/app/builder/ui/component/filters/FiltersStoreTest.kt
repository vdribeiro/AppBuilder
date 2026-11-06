package com.app.builder.ui.component.filters

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import com.app.builder.data.storage.AppFile
import com.app.builder.test.TestCase

class FiltersStoreTest: TestCase() {

    /** Filters the task list seeds its preferences with. */
    private val defaults = AppFile.ActionBarData(
        search = "",
        searchableProperties = listOf("TITLE", "DESCRIPTION"),
        sortAscending = false,
        sortProperty = "MODIFIED_AT",
        visibleProperties = listOf("TITLE", "DESCRIPTION", "STATE"),
    )

    /**
     * Keeps the store's state observed, as a rendered action bar would.
     *
     * @param store The store to observe.
     * @return The collecting [Job].
     */
    private fun TestScope.subscribe(store: FiltersStore): Job = backgroundScope.launch { store.stateFlow.collect {} }

    /**
     * Creates a store persisting to the task preferences.
     *
     * @return The [FiltersStore] instance.
     */
    private fun taskFiltersStore(): FiltersStore = FiltersStore(state = FiltersState(), storageFile = AppFile.TaskPreferences, defaults = defaults)

    /** Verifies that with nothing persisted yet, the defaults are both applied to the state and written to the storage file. */
    @Test
    fun seedDefaults() = runUnitTest {
        val store = taskFiltersStore()
        testScheduler.advanceUntilIdle()

        assertEquals(expected = defaults, actual = AppFile.TaskPreferences.load())
        assertEquals(expected = defaults.searchableProperties, actual = store.state.searchableProperties)
        assertEquals(expected = defaults.sortAscending, actual = store.state.sortAscending)
        assertEquals(expected = defaults.sortProperty, actual = store.state.sortProperty)
        assertEquals(expected = defaults.visibleProperties, actual = store.state.visibleProperties)
    }

    /** Verifies that a partially persisted payload keeps its own values and has only its missing ones backfilled with the defaults. */
    @Test
    fun backfillDefaults() = runUnitTest {
        AppFile.TaskPreferences.save { AppFile.ActionBarData(search = "query", sortAscending = true) }

        val store = taskFiltersStore()
        testScheduler.advanceUntilIdle()

        val expected = defaults.copy(search = "query", sortAscending = true)
        assertEquals(expected = expected, actual = AppFile.TaskPreferences.load())
        assertTrue(actual = store.state.sortAscending)
        assertEquals(expected = defaults.sortProperty, actual = store.state.sortProperty)
        assertEquals(expected = defaults.searchableProperties, actual = store.state.searchableProperties)
        assertEquals(expected = defaults.visibleProperties, actual = store.state.visibleProperties)
    }

    /** Verifies that a fully persisted payload is applied as is, without being overwritten by the defaults. */
    @Test
    fun keepPersisted() = runUnitTest {
        val persisted = AppFile.ActionBarData(
            search = "query",
            searchableProperties = listOf("STATE"),
            sortAscending = true,
            sortProperty = "TITLE",
            visibleProperties = listOf("TITLE"),
        )
        AppFile.TaskPreferences.save { persisted }

        val store = taskFiltersStore()
        testScheduler.advanceUntilIdle()

        assertEquals(expected = persisted, actual = AppFile.TaskPreferences.load())
        assertEquals(expected = persisted.searchableProperties, actual = store.state.searchableProperties)
        assertEquals(expected = persisted.sortProperty, actual = store.state.sortProperty)
        assertEquals(expected = persisted.visibleProperties, actual = store.state.visibleProperties)
    }

    /** Verifies that searching, sorting and toggling properties update the state and persist every change to the storage file. */
    @Test
    fun persistChanges() = runUnitTest {
        val store = taskFiltersStore()
        testScheduler.advanceUntilIdle()

        store.send(action = FiltersAction.Search(search = "query"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = "query", actual = AppFile.TaskPreferences.load()?.search)

        store.send(action = FiltersAction.SelectSortOrder(ascending = true))
        testScheduler.advanceUntilIdle()
        assertTrue(actual = store.state.sortAscending)
        assertEquals(expected = true, actual = AppFile.TaskPreferences.load()?.sortAscending)

        store.send(action = FiltersAction.SelectSortProperty(property = "TITLE"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = "TITLE", actual = store.state.sortProperty)
        assertEquals(expected = "TITLE", actual = AppFile.TaskPreferences.load()?.sortProperty)

        store.send(action = FiltersAction.ToggleSearchableProperty(property = "TITLE"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = listOf("DESCRIPTION"), actual = store.state.searchableProperties)
        assertEquals(expected = listOf("DESCRIPTION"), actual = AppFile.TaskPreferences.load()?.searchableProperties)

        store.send(action = FiltersAction.ToggleSearchableProperty(property = "TITLE"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = listOf("DESCRIPTION", "TITLE"), actual = store.state.searchableProperties)
        assertEquals(expected = listOf("DESCRIPTION", "TITLE"), actual = AppFile.TaskPreferences.load()?.searchableProperties)

        store.send(action = FiltersAction.ToggleVisibleProperty(property = "STATE"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = listOf("TITLE", "DESCRIPTION"), actual = store.state.visibleProperties)
        assertEquals(expected = listOf("TITLE", "DESCRIPTION"), actual = AppFile.TaskPreferences.load()?.visibleProperties)

        store.send(action = FiltersAction.ShowDeleted(showDeleted = true))
        testScheduler.advanceUntilIdle()
        assertTrue(actual = store.state.showDeleted)
        assertEquals(expected = true, actual = AppFile.TaskPreferences.load()?.showDeleted)

        val persisted = AppFile.TaskPreferences.load()
        assertEquals(expected = "query", actual = persisted?.search)
        assertEquals(expected = true, actual = persisted?.sortAscending)
        assertEquals(expected = "TITLE", actual = persisted?.sortProperty)
    }

    /** Verifies that expanding the filters persists it and that another store on the same file follows it. */
    @Test
    fun expandFiltersFollowsFile() = runUnitTest {
        val store = taskFiltersStore()
        val otherStore = taskFiltersStore()
        subscribe(store = store)
        subscribe(store = otherStore)
        testScheduler.advanceUntilIdle()
        assertFalse(actual = otherStore.state.filtersExpanded)

        store.send(action = FiltersAction.ExpandFilters(expanded = true))
        testScheduler.advanceUntilIdle()
        assertTrue(actual = store.state.filtersExpanded)
        assertEquals(expected = true, actual = AppFile.TaskPreferences.load()?.filtersExpanded)
        assertTrue(actual = otherStore.state.filtersExpanded)

        otherStore.send(action = FiltersAction.ExpandFilters(expanded = false))
        testScheduler.advanceUntilIdle()
        assertFalse(actual = store.state.filtersExpanded)
    }

    /** Verifies that the search query and opening the search input persist and that another store on the same file follows them. */
    @Test
    fun searchFollowsFile() = runUnitTest {
        val store = taskFiltersStore()
        val otherStore = taskFiltersStore()
        subscribe(store = store)
        subscribe(store = otherStore)
        testScheduler.advanceUntilIdle()

        store.send(action = FiltersAction.ExpandSearch(expanded = true))
        store.send(action = FiltersAction.Search(search = "query"))
        testScheduler.advanceUntilIdle()
        assertTrue(actual = store.state.searchExpanded)
        assertEquals(expected = "query", actual = store.state.search)
        assertEquals(expected = true, actual = AppFile.TaskPreferences.load()?.searchExpanded)
        assertTrue(actual = otherStore.state.searchExpanded)
        assertEquals(expected = "query", actual = otherStore.state.search)

        otherStore.send(action = FiltersAction.ExpandSearch(expanded = false))
        otherStore.send(action = FiltersAction.Search(search = ""))
        testScheduler.advanceUntilIdle()
        assertFalse(actual = store.state.searchExpanded)
        assertEquals(expected = "", actual = store.state.search)
    }

    /** Verifies that without a storage file the sorting and property filters still apply to the state. */
    @Test
    fun inMemory() = runUnitTest {
        val store = FiltersStore(state = FiltersState(visibleProperties = persistentListOf("TITLE")))

        store.send(action = FiltersAction.SelectSortOrder(ascending = false))
        store.send(action = FiltersAction.SelectSortProperty(property = "STATE"))
        store.send(action = FiltersAction.ToggleVisibleProperty(property = "STATE"))
        testScheduler.advanceUntilIdle()

        assertFalse(actual = store.state.sortAscending)
        assertEquals(expected = "STATE", actual = store.state.sortProperty)
        assertEquals(expected = listOf("TITLE", "STATE"), actual = store.state.visibleProperties)
        assertNull(actual = AppFile.TaskPreferences.load())
    }
}
