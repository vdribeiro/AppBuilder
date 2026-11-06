package com.app.builder.ui.component.actionbar

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import com.app.builder.ui.core.image.Image

/** Actions that can be dispatched to an action bar store. */
sealed interface ActionBarAction {
    /** Opens the user profile screen. */
    data object OpenUser: ActionBarAction
    /** Add a domain. */
    data object Add: ActionBarAction
    /** Start editing the current domain. */
    data object Edit: ActionBarAction
    /** Save the current domain, leaving whether to stop editing it to [SetEditing]. */
    data object Save: ActionBarAction
    /** Discard the edits to the current domain and stop editing it. */
    data object Cancel: ActionBarAction
    /**
     * Follows whether the current domain is being edited, as decided by its screen.
     *
     * @param editing Whether the current domain is being edited.
     */
    data class SetEditing(val editing: Boolean): ActionBarAction
    /** Delete the current domain. */
    data object Delete: ActionBarAction
    /** Restore the current domain. */
    data object Restore: ActionBarAction
    /** Opens the login screen. */
    data object Login: ActionBarAction
    /**
     * Expands or collapses the filters.
     *
     * @param expanded true to expand the filters, false to collapse them.
     */
    data class ExpandFilters(val expanded: Boolean): ActionBarAction
    /**
     * Opens or closes the search input.
     *
     * @param expanded true to open the search input, false to close it.
     */
    data class ExpandSearch(val expanded: Boolean): ActionBarAction
    /**
     * Updates the current search query.
     *
     * @param search New search query text.
     */
    data class Search(val search: String): ActionBarAction
    /**
     * Notifies the properties that are searchable.
     *
     * @param property Key of the property to toggle searchability.
     */
    data class ToggleSearchableProperty(val property: String): ActionBarAction
    /**
     * Selects sort direction.
     *
     * @param ascending true if sort is ascending, false for descending.
     */
    data class SelectSortOrder(val ascending: Boolean): ActionBarAction
    /**
     * Selects the property used to sort the list.
     *
     * @param property Key of the property to sort by.
     */
    data class SelectSortProperty(val property: String): ActionBarAction
    /**
     * Notifies the properties that are visible.
     *
     * @param property Key of the property to toggle visibility.
     */
    data class ToggleVisibleProperty(val property: String): ActionBarAction
    /**
     * Shows or hides the deleted items.
     *
     * @param showDeleted true to show deleted items, false to hide them.
     */
    data class ShowDeleted(val showDeleted: Boolean): ActionBarAction
}

/**
 * State of an action bar, holding what it displays and the sorting and property filters it applies to the screen below it.
 *
 * @property properties Map of property keys to their display labels, used to populate the sort, visibility and search menus.
 * @property avatarName The name to fit in the avatar. Only the first letter of the first and last name will show capitalized if no image is provided.
 * @property avatarImage The image resource.
 * @property synced Whether no background job is pending or running.
 * @property clockTrusted Whether the device clock can be trusted.
 * @property writable Whether the current user has write permission on the entity.
 * @property editable Whether the current user has write permission on the entity or on any other entity editable from the screen.
 * @property editing Whether the current domain is being edited.
 * @property showAdd Whether the add button should be shown.
 * @property showEdit Whether the edit button should be shown.
 * @property showSave Whether the save button should be shown.
 * @property showCancel Whether the cancel button should be shown.
 * @property showDelete Whether the delete toggle button should be shown.
 * @property showLogin Whether the login button should be shown.
 * @property showFilters Whether the filters button should be shown.
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
data class ActionBarState(
    val properties: ImmutableMap<String, String> = persistentMapOf(),
    val avatarName: String? = null,
    val avatarImage: Image? = null,
    val synced: Boolean = true,
    val clockTrusted: Boolean = true,
    val writable: Boolean = false,
    val editable: Boolean = false,
    val editing: Boolean = false,
    val showAdd: Boolean = false,
    val showEdit: Boolean = false,
    val showSave: Boolean = false,
    val showCancel: Boolean = false,
    val showDelete: Boolean = false,
    val showLogin: Boolean = false,
    val showFilters: Boolean = false,
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
