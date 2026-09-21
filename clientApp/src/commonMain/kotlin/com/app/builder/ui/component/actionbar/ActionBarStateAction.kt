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
    data object Back: ActionBarAction
    data object Login: ActionBarAction
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
 *
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
