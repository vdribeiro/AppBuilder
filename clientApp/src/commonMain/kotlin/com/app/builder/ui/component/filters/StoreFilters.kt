package com.app.builder.ui.component.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.component.bar.Filters
import com.app.builder.ui.component.bar.FiltersButton
import com.app.builder.ui.store.Store

/**
 * The button expanding and collapsing the filters of [filtersStore].
 *
 * @param filtersStore Store holding the list filters.
 */
@Composable
fun StoreFiltersButton(filtersStore: Store<FiltersState, FiltersAction>) {
    val filters by filtersStore.stateFlow.collectAsStateWithLifecycle()
    FiltersButton(
        expanded = filters.filtersExpanded,
        onClick = { filtersStore.send(action = FiltersAction.ExpandFilters(expanded = it)) }
    )
}

/**
 * The search, sorting and property filters of [filtersStore], applying every change to it.
 *
 * @param filtersStore Store holding the list filters.
 * @param showDeletedToggle Whether to offer the deleted items toggle, when the filters offer it too.
 */
@Composable
fun StoreFilters(filtersStore: Store<FiltersState, FiltersAction>, showDeletedToggle: Boolean = true) {
    val filters by filtersStore.stateFlow.collectAsStateWithLifecycle()
    Filters(
        properties = filters.properties,
        search = filters.search,
        searchExpanded = filters.searchExpanded,
        onSearchExpandClick = { filtersStore.send(action = FiltersAction.ExpandSearch(expanded = it)) },
        onSearch = { filtersStore.send(action = FiltersAction.Search(search = it)) },
        searchableProperties = filters.searchableProperties,
        onSearchablePropertiesChange = { filtersStore.send(action = FiltersAction.ToggleSearchableProperty(property = it)) },
        sortAscending = filters.sortAscending,
        onSortAscendingClick = { filtersStore.send(action = FiltersAction.SelectSortOrder(ascending = it)) },
        sortProperty = filters.sortProperty,
        onSelectSortProperty = { filtersStore.send(action = FiltersAction.SelectSortProperty(property = it)) },
        visibleProperties = filters.visibleProperties,
        onVisiblePropertiesChange = { filtersStore.send(action = FiltersAction.ToggleVisibleProperty(property = it)) },
        showDeleted = filters.showDeleted,
        onShowDeletedClick = if (showDeletedToggle && filters.showDeletedToggle) {
            { filtersStore.send(action = FiltersAction.ShowDeleted(showDeleted = it)) }
        } else null,
    )
}
