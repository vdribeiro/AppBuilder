package com.app.builder.ui.screen.taskdetail

import kotlinx.coroutines.Job
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Task
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.ui.component.Store

/**
 * Store backing the Task Detail Screen, observing the task and the action bar's edit mode, and applying title/description edits.
 *
 * @param state Initial Task Detail Screen state.
 * @property taskUseCases Use cases used to observe the task.
 * @property taskUuid UUID of the task being displayed.
 */
class TaskDetailScreenStore(
    state: TaskDetailScreenState,
    private val taskUseCases: TaskUseCases,
    private val taskUuid: String
): Store<TaskDetailScreenState, TaskDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /** Observes the task with [taskUuid], creating a placeholder if it doesn't exist yet, and observes the cached action bar state to keep edit mode in sync. */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = taskUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid task uuid")
            return@launch
        }

        taskUseCases.observeTask(uuid = uuid).observe(id = "task") { task ->
            val task = task ?: Task(
                uuid = uuid,
                modifiedAt = now(),
                deletedAt = null,
                title = "",
                description = "",
                state = Task.State.TODO
            )
            updateState { it.copy(task = task) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: TaskDetailScreenState, action: TaskDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TaskDetailScreenAction.ChangeTitle -> updateState { it.copy(task = it.task?.copy(title = action.title)) }
            is TaskDetailScreenAction.ChangeDescription -> updateState { it.copy(task = it.task?.copy(description = action.description)) }
        }
    }

    companion object {
        private const val TAG = "TaskDetailScreenStore"
    }
}
