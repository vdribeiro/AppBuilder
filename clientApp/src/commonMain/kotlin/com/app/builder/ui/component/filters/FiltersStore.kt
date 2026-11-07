package com.app.builder.ui.component.filters

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.data.storage.StorageFile
import com.app.builder.plusOrMinus
import com.app.builder.ui.store.Store

/**
 * Store holding the search, sorting and property filters of a list, persisting them and following every change to the persisted file.
 * It starts from the file's cached value when there is one, so the filters show as persisted from the first frame.
 *
 * @param state Initial filters state.
 * @property storageFile File the filters are persisted to, or `null` to keep them in memory only.
 * @property defaults Filters used when [storageFile] holds nothing yet.
 */
class FiltersStore(
    state: FiltersState,
    private val storageFile: StorageFile<AppFile.ActionBarData>? = null,
    private val defaults: AppFile.ActionBarData = AppFile.ActionBarData(),
): Store<FiltersState, FiltersAction>(initialState = state) {
    init {
        storageFile?.cacheFlow?.value?.let { persisted -> updateState { it.withData(data = persisted.withDefaults()) } }
        setup()
    }

    override fun reducer(state: FiltersState, action: FiltersAction) {
        super.reducer(state, action)
        when (action) {
            is FiltersAction.ExpandFilters -> expandFilters(action = action)
            is FiltersAction.ExpandSearch -> expandSearch(action = action)
            is FiltersAction.Search -> search(action = action)
            is FiltersAction.ToggleSearchableProperty -> toggleSearchableProperty(state = state, action = action)
            is FiltersAction.SelectSortOrder -> selectSortOrder(action = action)
            is FiltersAction.SelectSortProperty -> selectSortProperty(action = action)
            is FiltersAction.ToggleVisibleProperty -> toggleVisibleProperty(state = state, action = action)
            is FiltersAction.ShowDeleted -> showDeleted(action = action)
        }
    }

    /**
     * Completes the persisted filters with the [defaults] and follows the file, so every store on the same file stays in step.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        storageFile?.run {
            val persisted = load()
            val data = persisted.withDefaults()
            if (data != persisted) save { data }
            updateState { it.withData(data = data) }

            cache().mapNotNull { it }.distinctUntilChanged().observe(id = "filters_data") { data ->
                updateState { it.withData(data = data) }
            }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Backfills every value this payload is missing with the [defaults], so a payload persisted before a value existed does not leave the list unsorted or unsearchable.
     *
     * @return The persisted payload completed with the [defaults], or the [defaults] when nothing is persisted yet.
     */
    private fun AppFile.ActionBarData?.withDefaults(): AppFile.ActionBarData = when (this) {
        null -> defaults
        else -> copy(
            searchableProperties = searchableProperties.ifEmpty { defaults.searchableProperties },
            sortProperty = sortProperty.ifBlank { defaults.sortProperty },
            visibleProperties = visibleProperties.ifEmpty { defaults.visibleProperties }
        )
    }

    /**
     * Applies the persisted filters.
     *
     * @param data The persisted filters.
     * @return This state with the search, sorting, property filters and expansions of [data].
     */
    private fun FiltersState.withData(data: AppFile.ActionBarData): FiltersState = copy(
        search = data.search,
        searchExpanded = data.searchExpanded,
        searchableProperties = data.searchableProperties.toPersistentList(),
        sortAscending = data.sortAscending,
        sortProperty = data.sortProperty,
        visibleProperties = data.visibleProperties.toPersistentList(),
        showDeleted = data.showDeleted,
        filtersExpanded = data.filtersExpanded,
    )

    /**
     * Applies and persists whether the filters are expanded.
     *
     * @param action Action carrying whether the filters are expanded.
     * @return The [Job] representing this execution.
     */
    private fun expandFilters(action: FiltersAction.ExpandFilters): Job = launch(id = "expandFilters") {
        updateState { it.copy(filtersExpanded = action.expanded) }
        storageFile?.save { (it ?: defaults).copy(filtersExpanded = action.expanded) }
    }

    /**
     * Applies and persists whether the search input is open.
     *
     * @param action Action carrying whether the search input is open.
     * @return The [Job] representing this execution.
     */
    private fun expandSearch(action: FiltersAction.ExpandSearch): Job = launch(id = "expandSearch") {
        updateState { it.copy(searchExpanded = action.expanded) }
        storageFile?.save { (it ?: defaults).copy(searchExpanded = action.expanded) }
    }

    /**
     * Applies and persists the current search query.
     *
     * @param action Action carrying the new search query text.
     * @return The [Job] representing this execution.
     */
    private fun search(action: FiltersAction.Search): Job = launch(id = "search") {
        updateState { it.copy(search = action.search) }
        storageFile?.save { (it ?: defaults).copy(search = action.search) }
    }

    /**
     * Adds or removes a property from the searchable ones, then persists the result.
     *
     * @param state Current filters state.
     * @param action Action carrying the key of the property to toggle.
     * @return The [Job] representing this execution.
     */
    private fun toggleSearchableProperty(state: FiltersState, action: FiltersAction.ToggleSearchableProperty): Job = launch(id = "toggleSearchableProperty") {
        val searchableProperties = state.searchableProperties.plusOrMinus(element = action.property).toPersistentList()
        updateState { it.copy(searchableProperties = searchableProperties) }
        storageFile?.save { (it ?: defaults).copy(searchableProperties = searchableProperties) }
    }

    /**
     * Applies and persists the direction the list is sorted in.
     *
     * @param action Action carrying the new sort direction.
     * @return The [Job] representing this execution.
     */
    private fun selectSortOrder(action: FiltersAction.SelectSortOrder): Job = launch(id = "selectSortOrder") {
        updateState { it.copy(sortAscending = action.ascending) }
        storageFile?.save { (it ?: defaults).copy(sortAscending = action.ascending) }
    }

    /**
     * Applies and persists the property the list is sorted by.
     *
     * @param action Action carrying the key of the property to sort by.
     * @return The [Job] representing this execution.
     */
    private fun selectSortProperty(action: FiltersAction.SelectSortProperty): Job = launch(id = "selectSortProperty") {
        updateState { it.copy(sortProperty = action.property) }
        storageFile?.save { (it ?: defaults).copy(sortProperty = action.property) }
    }

    /**
     * Adds or removes a property from the visible ones, then persists the result.
     *
     * @param state Current filters state.
     * @param action Action carrying the key of the property to toggle.
     * @return The [Job] representing this execution.
     */
    private fun toggleVisibleProperty(state: FiltersState, action: FiltersAction.ToggleVisibleProperty): Job = launch(id = "toggleVisibleProperty") {
        val visibleProperties = state.visibleProperties.plusOrMinus(element = action.property).toPersistentList()
        updateState { it.copy(visibleProperties = visibleProperties) }
        storageFile?.save { (it ?: defaults).copy(visibleProperties = visibleProperties) }
    }

    /**
     * Applies and persists whether deleted items are shown.
     *
     * @param action Action carrying whether deleted items are shown.
     * @return The [Job] representing this execution.
     */
    private fun showDeleted(action: FiltersAction.ShowDeleted): Job = launch(id = "showDeleted") {
        updateState { it.copy(showDeleted = action.showDeleted) }
        storageFile?.save { (it ?: defaults).copy(showDeleted = action.showDeleted) }
    }

    companion object {
        private const val TAG = "FiltersStore"
    }
}
