package com.app.builder.ui.component.actionbar

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.core.image.Image

/** Actions that can be dispatched to an action bar store. */
sealed interface ActionBarAction {
    /** Opens the user profile screen. */
    data object OpenUser: ActionBarAction
    /**
     * Notifies that the mode has changed.
     *
     * @param mode The new mode it has changed to.
     */
    data class ModeChange(val mode: ActionBarMode): ActionBarAction
    /**
     * Updates the current search query.
     *
     * @param search New search query text.
     */
    data class Search(val search: String): ActionBarAction
    /**
     * Selects the property used to sort the list.
     *
     * @param property Key of the property to sort by.
     */
    data class SelectSortProperty(val property: String): ActionBarAction
    /**
     * Selects sort direction.
     *
     * @param ascending true if sort is ascending, false for descending.
     */
    data class SelectSortOrder(val ascending: Boolean): ActionBarAction
    /**
     * Notifies the properties that are visible.
     *
     * @param property Key of the property to toggle visibility.
     */
    data class ToggleVisibleProperty(val property: String): ActionBarAction
    /**
     * Notifies the properties that are searchable.
     *
     * @param property Key of the property to toggle searchability.
     */
    data class ToggleSearchableProperty(val property: String): ActionBarAction
    /**
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): ActionBarAction
    /**
     * Cancels the current pending mode and returns to the default layout.
     *
     * @param mode The pending mode.
     */
    data class Cancel(val mode: ActionBarMode): ActionBarAction
}

/**
 * State of an action bar, holding what it displays and the sorting and property filters it applies to the screen below it.
 *
 * @property title Text shown as the action bar's title.
 * @property avatarName The name to fit in the avatar. Only the first letter of the first and last name will show capitalized if no image is provided.
 * @property avatarImage The image resource.
 * @property mode Current display mode, controlling which actions/inputs are shown.
 * @property layout Which set of actions to show, based on whether the screen displays a list, a detail, or both.
 * @property write Whether buttons that allow "write" operations should be shown.
 * @property sortProperty Key of the property currently used to sort the list.
 * @property sortAscending Whether the list is currently sorted in ascending order.
 * @property properties Map of property keys to their display labels, used to populate the sort, visibility and search menus.
 * @property visibleProperties Property keys currently shown as visible columns.
 * @property searchableProperties Property keys currently included when searching.
 */
data class ActionBarState(
    val title: String = "",
    val avatarName: String? = null,
    val avatarImage: Image? = null,
    val mode: ActionBarMode = ActionBarMode.DEFAULT,
    val layout: ActionBarLayout = ActionBarLayout.NONE,
    val write: Boolean = false,
    val sortProperty: String = "",
    val sortAscending: Boolean = true,
    val properties: ImmutableMap<String, String> = persistentMapOf(),
    val visibleProperties: ImmutableList<String> = persistentListOf(),
    val searchableProperties: ImmutableList<String> = persistentListOf(),
)
