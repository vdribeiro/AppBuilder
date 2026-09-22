package com.app.builder.ui.screen.tasklist

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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