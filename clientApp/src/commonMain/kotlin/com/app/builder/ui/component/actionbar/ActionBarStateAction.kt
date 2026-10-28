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
    /** Save the current domain. */
    data object Save: ActionBarAction
    /** Delete the current domain. */
    data object Delete: ActionBarAction
    /** Opens the login screen. */
    data object Login: ActionBarAction
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
}

/**
 * State of an action bar, holding what it displays and the sorting and property filters it applies to the screen below it.
 *
 * @property properties Map of property keys to their display labels, used to populate the sort, visibility and search menus.
 * @property avatarName The name to fit in the avatar. Only the first letter of the first and last name will show capitalized if no image is provided.
 * @property avatarImage The image resource.
 * @property synced Whether no background job is pending or running, or `null` when the status isn't followed.
 * @property clockTrusted Whether the device clock can be trusted.
 * @property showAdd Whether the add button should be shown.
 * @property showSave Whether the save button should be shown.
 * @property showDelete Whether the delete button should be shown.
 * @property showLogin Whether the login button should be shown.
 * @property searchableProperties Property keys currently included when searching.
 * @property sortAscending Whether the list is currently sorted in ascending order.
 * @property sortProperty Key of the property currently used to sort the list.
 * @property visibleProperties Property keys currently shown as visible columns.
 */
data class ActionBarState(
    val properties: ImmutableMap<String, String> = persistentMapOf(),
    val avatarName: String? = null,
    val avatarImage: Image? = null,
    val synced: Boolean? = null,
    val clockTrusted: Boolean = true,
    val showAdd: Boolean = false,
    val showSave: Boolean = false,
    val showDelete: Boolean = false,
    val showLogin: Boolean = false,
    val searchableProperties: ImmutableList<String> = persistentListOf(),
    val sortAscending: Boolean = true,
    val sortProperty: String = "",
    val visibleProperties: ImmutableList<String> = persistentListOf(),
)
