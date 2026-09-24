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
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Task Detail Screen, observing the task and the action bar's edit mode, and applying title/description edits.
 *
 * @param state Initial Task Detail Screen state.
 * @property router The router used for navigation.
 * @property taskUseCases Use cases used to observe the task.
 * @property taskUuid UUID of the task being displayed.
 */
class TaskDetailScreenStore(
    state: TaskDetailScreenState,
    private val router: Router,
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
            is TaskDetailScreenAction.Cancel -> cancel(state = state, action = action)
        }
    }

    /**
     * Saves the edited task when the confirmed mode is one that edits it, and does nothing for the modes that do not.
     *
     * @param state Current task detail state, holding the task being edited.
     * @param action Action carrying the mode being confirmed.
     * @return The [Job] representing this execution.
     */
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

    private fun cancel(state: TaskDetailScreenState, action: TaskDetailScreenAction.Cancel): Job = launch(id = "cancel") {
        when (action.mode) {
            ActionBarMode.DEFAULT -> TODO()
            ActionBarMode.SEARCH -> TODO()
            ActionBarMode.ADD -> router.back()
            ActionBarMode.EDIT -> TODO()
            ActionBarMode.DELETE -> TODO()
            ActionBarMode.BATCH_DELETE -> TODO()
        }
        updateState { it.copy(editMode = false) }
    }

    companion object {
        private const val TAG = "TaskDetailScreenStore"
    }
}
