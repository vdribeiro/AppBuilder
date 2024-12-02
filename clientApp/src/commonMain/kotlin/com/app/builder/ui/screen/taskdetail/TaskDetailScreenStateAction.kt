package com.app.builder.ui.screen.taskdetail

import com.app.builder.domain.Task

/** Actions supported by the Task Detail Screen. */
sealed interface TaskDetailScreenAction {
    /**
     * Updates the task's title.
     *
     * @param title The new title value.
     */
    data class ChangeTitle(val title: String): TaskDetailScreenAction
    /**
     * Updates the task's description.
     *
     * @param description The new description value.
     */
    data class ChangeDescription(val description: String): TaskDetailScreenAction
}

/**
 * State of the Task Detail Screen.
 *
 * @property task Task being viewed or edited, or `null` while it's still loading.
 * @property editMode Whether the screen is in edit mode.
 */
data class TaskDetailScreenState(
    val task: Task? = null,
    val editMode: Boolean = false,
)
