package com.app.builder.ui.screen.tasklist

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Task
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
     * Confirms and commits the current pending mode.
     *
     * @param mode The pending mode.
     */
    data class Ok(val mode: ActionBarMode): TaskListScreenAction
}

val defaultFilterCriteria = AppFile.ActionBarData(
    mode = ActionBarMode.DEFAULT.name,
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
 * @property mode Current action bar mode, controlling how a task selection is handled.
 * @property selectedUuids UUIDs of the tasks picked while in batch delete mode.
 */
data class TaskListScreenState(
    val tasks: ImmutableList<TaskItem> = persistentListOf(),
    val mode: ActionBarMode = ActionBarMode.DEFAULT,
    val selectedUuids: ImmutableList<Uuid> = persistentListOf()
)

/**
 * Slice of [TaskListScreenState] the displayed task list is derived from, kept separate so unrelated state changes do not re-filter the list.
 *
 * @property selectedUuids UUIDs of the tasks picked while in batch delete mode.
 */
data class TaskListFilterCriteria(
    val selectedUuids: ImmutableList<Uuid>
)

/**
 * Groups the inputs the displayed task list is built from.
 *
 * @property tasks Tasks observed from storage, before filtering and sorting.
 * @property actionBarData Persisted action bar data holding the search query, sorting and property filters.
 * @property criteria Criteria derived from the current screen state.
 */
data class TaskListActionBarFilterCombine(
    val tasks: List<Task>,
    val actionBarData: AppFile.ActionBarData,
    val criteria: TaskListFilterCriteria
)