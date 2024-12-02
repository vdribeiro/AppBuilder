package com.app.builder.ui.screen.taskdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.Task
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.Store
import com.app.builder.ui.component.card.TaskCard
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.screen.Screen

/**
 * The user-facing Task Detail Screen, showing a single task's title and description, or a progress indicator while the task has not yet loaded.
 *
 * @param store Provides the task detail state and receives its actions.
 * @param navigationStore Drives the bottom navigation bar.
 */
@Composable
fun TaskDetailScreen(
    store: Store<TaskDetailScreenState, TaskDetailScreenAction>,
    navigationStore: Store<NavigationState, Unit>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val task = state.task

    Screen(
        bottomBar = { Navigation(store = navigationStore) }
    ) {
        TaskCard(
            enabled = task != null && state.editMode,
            title = task?.title,
            onTitleChange = { store.send(action = TaskDetailScreenAction.ChangeTitle(title = it)) },
            description = task?.description,
            onDescriptionChange = { store.send(action = TaskDetailScreenAction.ChangeDescription(description = it)) },
        )
    }
}

@Preview
@Composable
private fun TaskDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "title" to "Title"
        )
    )
    TaskDetailScreen(
        store = Store(
            initialState = TaskDetailScreenState(
                task = Task(
                    uuid = uuid(),
                    modifiedAt = now(),
                    deletedAt = null,
                    title = "Big Title",
                    description = "A description lalala",
                    state = Task.State.TODO
                )
            )
        ),
        navigationStore = Store(initialState = NavigationState())
    )
}
