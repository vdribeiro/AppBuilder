package com.app.builder.ui.component.filters

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/** Actions that can be dispatched to a filters store. */
sealed interface FiltersAction {
    /**
     * Expands or collapses the filters.
     *
     * @param expanded true to expand the filters, false to collapse them.
     */
    data class ExpandFilters(val expanded: Boolean): FiltersAction
    /**
     * Opens or closes the search input.
     *
     * @param expanded true to open the search input, false to close it.
     */
    data class ExpandSearch(val expanded: Boolean): FiltersAction
    /**
     * Updates the current search query.
     *
     * @param search New search query text.
     */
    data class Search(val search: String): FiltersAction
    /**
     * Notifies the properties that are searchable.
     *
     * @param property Key of the property to toggle searchability.
     */
    data class ToggleSearchableProperty(val property: String): FiltersAction
    /**
     * Selects sort direction.
     *
     * @param ascending true if sort is ascending, false for descending.
     */
    data class SelectSortOrder(val ascending: Boolean): FiltersAction
    /**
     * Selects the property used to sort the list.
     *
     * @param property Key of the property to sort by.
     */
    data class SelectSortProperty(val property: String): FiltersAction
    /**
     * Notifies the properties that are visible.
     *
     * @param property Key of the property to toggle visibility.
     */
    data class ToggleVisibleProperty(val property: String): FiltersAction
    /**
     * Shows or hides the deleted items.
     *
     * @param showDeleted true to show deleted items, false to hide them.
     */
    data class ShowDeleted(val showDeleted: Boolean): FiltersAction
}

/**
 * State of the search, sorting and property filters applied to a list.
 *
 * @property properties Map of property keys to their display labels, used to populate the sort, visibility and search menus.
 * @property filtersExpanded Whether the filters are currently expanded.
 * @property search Current search query text.
 * @property searchExpanded Whether the search input is currently open.
 * @property searchableProperties Property keys currently included when searching.
 * @property sortAscending Whether the list is currently sorted in ascending order.
 * @property sortProperty Key of the property currently used to sort the list.
 * @property visibleProperties Property keys currently shown as visible columns.
 * @property showDeletedToggle Whether the deleted items toggle should be shown among the filters.
 * @property showDeleted Whether deleted items are currently shown.
 */
data class FiltersState(
    val properties: ImmutableMap<String, String> = persistentMapOf(),
    val filtersExpanded: Boolean = false,
    val search: String = "",
    val searchExpanded: Boolean = false,
    val searchableProperties: ImmutableList<String> = persistentListOf(),
    val sortAscending: Boolean = true,
    val sortProperty: String = "",
    val visibleProperties: ImmutableList<String> = persistentListOf(),
    val showDeletedToggle: Boolean = true,
    val showDeleted: Boolean = false,
)
