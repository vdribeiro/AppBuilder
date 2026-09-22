package com.app.builder.ui.screen.tasklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.TaskList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Task List Screen, showing the list of tasks.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the task list state and receives its actions.
 */
@Composable
fun TaskListScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<TaskListScreenState, TaskListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    Screen(
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        TaskList(
            items = state.tasks,
            onClick = { store.send(action = TaskListScreenAction.SelectTask(taskUuid = it.uuid)) }
        )
    }
}

@Preview
@Composable
private fun TaskListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "task_list" to "Tasks",
            "title" to "Title",
            "description" to "Description"
        )
    )
    TaskListScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Tasks")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(initialState = TaskListScreenState())
    )
}
