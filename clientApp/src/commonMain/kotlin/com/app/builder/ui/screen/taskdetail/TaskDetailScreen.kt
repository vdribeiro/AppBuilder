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
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.card.TaskCard
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Task Detail Screen, showing a single task.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the task detail state and receives its actions.
 */
@Composable
fun TaskDetailScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<TaskDetailScreenState, TaskDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val task = state.task

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.back() },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        TaskCard(
            enabled = task != null && state.editMode,
            modifiedAt = task?.modifiedAt?.toString(),
            deletedAt = task?.deletedAt?.toString(),
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
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Task")),
        navigationStore = Store(initialState = NavigationState()),
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
        )
    )
}
