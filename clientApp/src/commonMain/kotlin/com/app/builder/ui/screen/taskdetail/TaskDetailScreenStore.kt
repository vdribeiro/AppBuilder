package com.app.builder.ui.screen.taskdetail

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Task
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.store.Store

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

    /**
     * Observes the task with [taskUuid], creating a placeholder if it doesn't exist yet, and observes the cached action bar state to keep edit mode in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = taskUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid task uuid")
            return@launch
        }

        taskUseCases.observeTask(uuid = uuid).observe(id = "task") { task ->
            val editMode = task == null
            val task = task ?: Task(
                uuid = uuid,
                modifiedAt = now(),
                deletedAt = null,
                title = "",
                description = "",
                state = Task.State.TODO
            )
            updateState {
                it.copy(
                    task = task,
                    editMode = editMode
                )
            }
        }

        AppFile.TaskPreferences.cache()
            .mapNotNull { it?.mode }
            .distinctUntilChanged()
            .observe(id = "syncMode") { newMode ->
                val mode = newMode.toEnumOrNull<ActionBarMode>() ?: return@observe
                when (mode) {
                    ActionBarMode.DEFAULT,
                    ActionBarMode.SEARCH,
                    ActionBarMode.DELETE,
                    ActionBarMode.BATCH_DELETE -> updateState { it.copy(editMode = false) }

                    ActionBarMode.ADD,
                    ActionBarMode.EDIT -> updateState { it.copy(editMode = true) }
                }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: TaskDetailScreenState, action: TaskDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TaskDetailScreenAction.ChangeTitle -> updateState { it.copy(task = it.task?.copy(title = action.title)) }
            is TaskDetailScreenAction.ChangeDescription -> updateState { it.copy(task = it.task?.copy(description = action.description)) }
            is TaskDetailScreenAction.Ok -> ok(state = state, action = action)
            is TaskDetailScreenAction.Cancel -> updateState { it.copy(editMode = false) }
        }
    }

    private fun ok(state: TaskDetailScreenState, action: TaskDetailScreenAction.Ok): Job = launch(id = "ok") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.DELETE,
            ActionBarMode.BATCH_DELETE -> Unit

            ActionBarMode.ADD,
            ActionBarMode.EDIT -> state.task?.let { taskUseCases.upsertTask(task = it) }
        }
    }

    companion object {
        private const val TAG = "TaskDetailScreenStore"
    }
}
