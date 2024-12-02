package com.app.builder.ui.screen.tasklist

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Task
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.list.TaskItem

/** Actions that can be dispatched to a task list store. */
sealed interface TaskListScreenAction {
    /**
     * Selects a task, either navigating to its detail screen or toggling its selection when in delete mode.
     *
     * @param taskUuid UUID of the task that was selected.
     */
    data class SelectTask(val taskUuid: String): TaskListScreenAction
    /**
     * Notifies that the mode has changed.
     *
     * @param old The mode before the change.
     * @param new The new mode it has changed to.
     */
    data class ModeChange(val old: ActionBarMode, val new: ActionBarMode): TaskListScreenAction
    /**
     * Updates the current search query.
     *
     * @param search New search query text.
     */
    data class Search(val search: String): TaskListScreenAction
    /**
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): TaskListScreenAction
    /**
     * Cancels the current pending mode and returns to the default layout.
     *
     * @param mode The pending mode.
     */
    data class Cancel(val mode: ActionBarMode): TaskListScreenAction
    /**
     * Selects the property used to sort the list.
     *
     * @param property Key of the property to sort by.
     */
    data class SelectSortProperty(val property: String): TaskListScreenAction
    /**
     * Selects sort direction.
     *
     * @param ascending true if sort is ascending, false for descending.
     */
    data class SelectSortOrder(val ascending: Boolean): TaskListScreenAction
    /**
     * Notifies the properties that are visible.
     *
     * @param properties Keys of the visible properties.
     */
    data class VisibleProperties(val properties: ImmutableList<String>): TaskListScreenAction
    /**
     * Notifies the properties that are searchable.
     *
     * @param properties Keys of the visible properties.
     */
    data class SearchableProperties(val properties: ImmutableList<String>): TaskListScreenAction
}

/** Default values of task list filters. */
val defaultFilterCriteria = AppFile.FilterCriteria(
    search = "",
    sortProperty = Task.Property.MODIFIED_AT.name,
    sortAscending = false,
    visibleProperties = Task.Property.entries.map { it.name }.toPersistentList(),
    searchableProperties = Task.Property.entries.map { it.name }.toPersistentList(),
)

/**
 * State of the task list, holding the tasks to display and whether batch delete selection is active.
 *
 * @property tasks Tasks currently displayed.
 * @property mode The current display mode.
 * @property layout Which actions to show.
 * @property write Whether buttons that allow "write" operations should be shown.
 * @property properties The map of property keys to their display labels, used to populate the sort, visibility and search menus.
 * @property filterCriteria Criteria used to filter, sort, and shape the task list.
 * @property onModeChange Called with the old and new mode whenever the display mode changes.
 * @property onSearch Called with the debounced search query as the user types in search mode.
 * @property onOkClick Called with the pending mode when the confirmation button is clicked.
 * @property onCancelClick Called with the pending mode when the cancellation button is clicked.
 * @property onSelectSortProperty Called with the selected property key when a sort-property menu item is clicked.
 * @property onSortAscendingClick Called with the new sort direction when the sort-direction button is clicked.
 * @property onVisibilityPropertiesChange Called with the updated set of visible property keys when one is toggled.
 * @property onSearchablePropertiesChange Called with the updated set of searchable property keys when one is toggled.
 */
data class TaskListScreenState(
    val tasks: ImmutableList<TaskItem> = persistentListOf(),
    val mode: ActionBarMode = ActionBarMode.DEFAULT,
    val layout: ActionBarLayout = ActionBarLayout.ALL,
    val write: Boolean = false,
    val properties: ImmutableMap<String, String> = persistentMapOf(),
    val filterCriteria: AppFile.FilterCriteria = defaultFilterCriteria,
    val onModeChange: (ActionBarMode, ActionBarMode) -> Unit = { _, _ -> },
    val onSearch: (String) -> Unit = {},
    val onOkClick: (ActionBarMode) -> Unit = {},
    val onCancelClick: (ActionBarMode) -> Unit = {},
    val onSelectSortProperty: (String) -> Unit = {},
    val onSortAscendingClick: (Boolean) -> Unit = {},
    val onVisibilityPropertiesChange: (ImmutableList<String>) -> Unit = {},
    val onSearchablePropertiesChange: (ImmutableList<String>) -> Unit = {},
)


