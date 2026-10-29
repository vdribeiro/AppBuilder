package com.app.builder.ui.screen.taskdetail

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Task
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Task Detail Screen, observing the task while viewing it, and applying, saving and discarding edits while editing it.
 *
 * @param state Initial Task Detail Screen state.
 * @property router The router used for navigation.
 * @property taskUseCases Use cases used to observe and save the task.
 * @property taskUuid UUID of the task being displayed.
 * @property new Whether the task is new and not stored yet, starting it in edit mode.
 */
class TaskDetailScreenStore(
    state: TaskDetailScreenState,
    private val router: Router,
    private val taskUseCases: TaskUseCases,
    private val taskUuid: String,
    private val new: Boolean = false
): Store<TaskDetailScreenState, TaskDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] task in edit mode, then observes the task with [taskUuid], ignoring its changes while it's being edited.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = taskUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid task uuid")
            return@launch
        }

        if (new) updateState { it.copy(task = Task(uuid = uuid), editing = true, new = true) }

        taskUseCases.observeTask(uuid = uuid).observe(id = "task") { task ->
            updateState { if (it.editing) it else it.copy(task = task) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: TaskDetailScreenState, action: TaskDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TaskDetailScreenAction.ChangeTitle -> updateState { it.copy(task = it.task?.copy(title = action.title)) }
            is TaskDetailScreenAction.ChangeDescription -> updateState { it.copy(task = it.task?.copy(description = action.description)) }
            is TaskDetailScreenAction.ChangeState -> updateState { it.copy(task = it.task?.copy(state = action.state)) }
            TaskDetailScreenAction.Edit -> updateState { it.copy(editing = true) }
            TaskDetailScreenAction.Save -> save(state = state)
            TaskDetailScreenAction.Cancel -> cancel(state = state)
            TaskDetailScreenAction.Delete -> delete(state = state)
        }
    }

    /**
     * Saves the edited task and stops editing it.
     *
     * @param state Task Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: TaskDetailScreenState): Job = launch(id = "save") {
        val task = state.task?.copy(modifiedAt = now()) ?: return@launch
        taskUseCases.upsertTask(task = task)
        updateState { it.copy(task = task, editing = false, new = false) }
    }

    /**
     * Discards the edits, then reloads the stored task and stops editing it, or leaves the screen when the task is new.
     *
     * @param state Task Detail Screen state at the time the cancel was requested.
     * @return The [Job] representing this execution.
     */
    private fun cancel(state: TaskDetailScreenState): Job = launch(id = "cancel") {
        if (state.new) {
            router.back()
            return@launch
        }
        val task = state.task?.let { taskUseCases.observeTask(uuid = it.uuid).firstOrNull() }
        updateState { it.copy(task = task, editing = false) }
    }

    private fun delete(state: TaskDetailScreenState): Job = launch(id = "delete") {
        state.task?.copy(deletedAt = now())?.let { taskUseCases.upsertTask(task = it) }
        router.back()
    }

    companion object {
        private const val TAG = "TaskDetailScreenStore"
    }
}
