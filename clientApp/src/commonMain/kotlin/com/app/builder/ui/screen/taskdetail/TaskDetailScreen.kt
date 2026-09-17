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
import com.app.builder.ui.component.bar.ActionBar
import com.app.builder.ui.component.card.TaskCard
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.useravatar.UserAvatar
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The user-facing Task Detail Screen, showing a single task.
 *
 * @param navigationStore Drives the bottom navigation bar.
 * @param userAvatarStore The store for user avatar.
 * @param store Provides the task detail state and receives its actions.
 */
@Composable
fun TaskDetailScreen(
    navigationStore: Store<NavigationState, Unit>,
    userAvatarStore: Store<UserAvatarState, Unit>,
    store: Store<TaskDetailScreenState, TaskDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val task = state.task

    Screen(
        bottomBar = { Navigation(store = navigationStore) },
        topBar = {
            ActionBar(
                title = "task",
                avatar = { UserAvatar(store = userAvatarStore) },
                mode = state.mode,
                layout = state.layout,
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
                visibilityProperties = state.filterCriteria.visibleProperties,
                onVisibilityPropertiesChange = state.onVisibilityPropertiesChange,
                searchableProperties = state.filterCriteria.searchableProperties,
                onSearchablePropertiesChange = state.onSearchablePropertiesChange
            )
        }
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
