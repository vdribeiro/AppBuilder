package com.app.builder.ui.screen.taskdetail

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Task Detail Screen, loading the task, observing the current user's write permission, and applying and saving edits.
 *
 * @param state Initial Task Detail Screen state.
 * @property router The router used for navigation.
 * @property authenticationUseCases Use cases used to observe the current user.
 * @property taskUseCases Use cases used to load and save the task.
 * @property taskUuid UUID of the task being displayed.
 */
class TaskDetailScreenStore(
    state: TaskDetailScreenState,
    private val router: Router,
    private val authenticationUseCases: AuthenticationUseCases,
    private val taskUseCases: TaskUseCases,
    private val taskUuid: String
): Store<TaskDetailScreenState, TaskDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the current user's write permission and loads the task with [taskUuid].
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        authenticationUseCases.observeCurrentUser().observe(id = "current_user") { user ->
            val editable = user?.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE) ?: false
            updateState { it.copy(editable = editable) }
        }

        val uuid = taskUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid task uuid")
            return@launch
        }

        val task = taskUseCases.observeTask(uuid = uuid).firstOrNull()
        updateState { it.copy(task = task, editedTask = task) }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: TaskDetailScreenState, action: TaskDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TaskDetailScreenAction.ChangeTitle -> updateState { it.copy(editedTask = it.editedTask?.copy(title = action.title)) }
            is TaskDetailScreenAction.ChangeDescription -> updateState { it.copy(editedTask = it.editedTask?.copy(description = action.description)) }
            TaskDetailScreenAction.Save -> save(state = state)
            TaskDetailScreenAction.Delete -> delete(state = state)
        }
    }

    /**
     * Saves the edited task and makes it the stored one.
     *
     * @param state Task Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: TaskDetailScreenState): Job = launch(id = "save") {
        val task = state.editedTask?.copy(modifiedAt = now()) ?: return@launch
        taskUseCases.upsertTask(task = task)
        updateState { it.copy(task = task, editedTask = task) }
    }

    private fun delete(state: TaskDetailScreenState): Job = launch(id = "delete") {
        state.task?.copy(deletedAt = now())?.let { taskUseCases.upsertTask(task = it) }
        router.back()
    }

    companion object {
        private const val TAG = "TaskDetailScreenStore"
    }
}
