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
    /**
     * Cancels the current pending mode and returns to the default layout.
     *
     * @param mode The pending mode.
     */
    data class Cancel(val mode: ActionBarMode): TaskListScreenAction
}

/** Default values of task list filters. */
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
 */
data class TaskListScreenState(
    val tasks: ImmutableList<TaskItem> = persistentListOf(),
    val mode: ActionBarMode = ActionBarMode.DEFAULT,
    val selectedUuids: ImmutableList<Uuid> = persistentListOf()
)

data class TaskListFilterCriteria(
    val selectedUuids: ImmutableList<Uuid>
)

data class TaskListActionBarFilterCombine(
    val tasks: List<Task>,
    val actionBarData: AppFile.ActionBarData,
    val criteria: TaskListFilterCriteria
)