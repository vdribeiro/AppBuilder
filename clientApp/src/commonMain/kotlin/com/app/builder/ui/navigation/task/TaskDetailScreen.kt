package com.app.builder.ui.navigation.task

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.domain.Task
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.card.TaskCard
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Task Detail Screen, showing a single task.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The task filters shared by every task screen, shown when the task list is beside this screen.
 * @param store Provides the task detail state and receives its actions.
 */
@Composable
fun TaskDetailScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<TaskDetailScreenState, TaskDetailScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val task = state.task
    val deleted = task?.deletedAt != null

    val router = LocalRouter.current
    val splitScreen = LocalSplitScreen.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore.takeIf { splitScreen },
                entityType = EntityType.TASK,
                deleted = deleted,
                onAddClick = if (splitScreen && !state.editing) {
                    { router.navigate(screen = Screen.TaskDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) }
                } else null,
                onEditClick = if (task != null && !state.editing && !deleted) {
                    { store.send(action = TaskDetailScreenAction.Edit) }
                } else null,
                onSaveClick = if (state.editing) {
                    { store.send(action = TaskDetailScreenAction.Save) }
                } else null,
                onCancelClick = if (state.editing) {
                    { store.send(action = TaskDetailScreenAction.Cancel) }
                } else null,
                onDeleteClick = if (task != null && !state.editing) {
                    { store.send(action = if (it) TaskDetailScreenAction.Delete else TaskDetailScreenAction.Restore) }
                } else null,
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.TASK) },
    ) {
        TaskCard(
            enabled = task != null && state.editing,
            uuid = task?.uuid?.toString(),
            modifiedAt = task?.modifiedAt?.toString(),
            deletedAt = task?.deletedAt?.toString(),
            title = task?.title,
            onTitleChange = { store.send(action = TaskDetailScreenAction.ChangeTitle(title = it)) },
            description = task?.description,
            onDescriptionChange = { store.send(action = TaskDetailScreenAction.ChangeDescription(description = it)) },
            state = task?.state?.translationKey,
            states = state.states,
            onStateChange = { key -> Task.State.entries.firstOrNull { it.translationKey == key }?.let { store.send(action = TaskDetailScreenAction.ChangeState(state = it)) } },
        )
    }
}

@PreviewLightDark
@Composable
private fun TaskDetailScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "title" to "Title"
        )
    )
    val task = Task(
        uuid = uuid(),
        modifiedAt = now(),
        deletedAt = null,
        title = "Big Title",
        description = "A description lalala",
        state = Task.State.TODO
    )
    TaskDetailScreen(
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(
            initialState = TaskDetailScreenState(
                task = task
            )
        )
    )
}
