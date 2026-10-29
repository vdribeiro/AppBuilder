package com.app.builder.ui.screen.tasklist

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.TaskItem

/** Actions that can be dispatched to a task list store. */
sealed interface TaskListScreenAction {
    /**
     * Selects a task, either navigating to its detail screen or toggling its selection when in delete mode.
     *
     * @param taskUuid UUID of the task that was selected.
     */
    data class SelectTask(val taskUuid: String): TaskListScreenAction
}

/**
 * State of the task list, holding the tasks to display and whether batch delete selection is active.
 *
 * @property tasks Tasks currently displayed.
 */
data class TaskListScreenState(
    val tasks: ImmutableList<TaskItem> = persistentListOf(),
)
