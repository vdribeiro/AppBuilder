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
    /**
     * Updates the task's state.
     *
     * @param state The new state value.
     */
    data class ChangeState(val state: Task.State): TaskDetailScreenAction
    /** Start editing the current task. */
    data object Edit: TaskDetailScreenAction
    /** Save the edited task and stop editing it. */
    data object Save: TaskDetailScreenAction
    /** Discard the edits, then reload the stored task and stop editing it, or leave the screen when the task is new. */
    data object Cancel: TaskDetailScreenAction
    /** Delete the current task. */
    data object Delete: TaskDetailScreenAction
}

/**
 * State of the Task Detail Screen.
 *
 * @property task Task being viewed or edited, or `null` while it's still loading.
 * @property editing Whether the task is being edited, which stops it from following the stored task.
 * @property new Whether the task is new and not stored yet.
 */
data class TaskDetailScreenState(
    val task: Task? = null,
    val editing: Boolean = false,
    val new: Boolean = false,
)
