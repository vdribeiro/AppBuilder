package com.app.builder.ui.screen.tasklist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.list.TaskList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.useravatar.UserAvatar
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Task List Screen, showing the list of tasks.
 *
 * @param navigationStore Drives the bottom navigation bar.
 * @param userAvatarStore The store for user avatar.
 * @param store Provides the task list state and receives its actions.
 */
@Composable
fun TaskListScreen(
    navigationStore: Store<NavigationState, Unit>,
    userAvatarStore: Store<UserAvatarState, Unit>,
    store: Store<TaskListScreenState, TaskListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val splitScreen = LocalSplitScreen.current
    val layout = if (splitScreen) ActionBarLayout.ALL else ActionBarLayout.DETAIL

    Screen(
        bottomBar = { Navigation(store = navigationStore) },
        topBar = {
            TopActionBar(
                title = "tasks",
                avatar = { UserAvatar(store = userAvatarStore) },
                mode = state.filterCriteria.mode,
                layout = layout,
                write = state.write,
                onModeChange = state.onModeChange,
                onSearch = state.onSearch,
                onOkClick = state.onOkClick,
                onCancelClick = state.onCancelClick,
                sortAscending = state.filterCriteria.sortAscending,
                sortProperty = state.filterCriteria.sortProperty,
                onSelectSortProperty = state.onSelectSortProperty,
                onSortAscendingClick = state.onSortAscendingClick,
                properties = state.properties,
                visibleProperties = state.filterCriteria.visibleProperties,
                onVisiblePropertiesChange = state.onVisibilityPropertiesChange,
                searchableProperties = state.filterCriteria.searchableProperties,
                onSearchablePropertiesChange = state.onSearchablePropertiesChange
            )
        }
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
        navigationStore = Store(initialState = NavigationState()),
        userAvatarStore = Store(initialState = UserAvatarState(userName = "Toto")),
        store = Store(initialState = TaskListScreenState())
    )
}
