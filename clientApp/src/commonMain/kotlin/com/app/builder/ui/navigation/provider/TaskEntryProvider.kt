package com.app.builder.ui.navigation.provider

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Task
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.LocalSplitScreen
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.bar.ActionBarLayout
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split
import com.app.builder.ui.screen.taskdetail.TaskDetailScreen
import com.app.builder.ui.screen.taskdetail.TaskDetailScreenAction
import com.app.builder.ui.screen.taskdetail.TaskDetailScreenState
import com.app.builder.ui.screen.taskdetail.TaskDetailScreenStore
import com.app.builder.ui.screen.tasklist.TaskListScreen
import com.app.builder.ui.screen.tasklist.TaskListScreenAction
import com.app.builder.ui.screen.tasklist.TaskListScreenState
import com.app.builder.ui.screen.tasklist.TaskListScreenStore

/**
 * The task routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.taskProvider(useCases: UseCases) {
    val properties = Task.Property.entries.map { it.name }.toPersistentList()
    val propertyMap = Task.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()
    val defaultFilterCriteria = AppFile.ActionBarData(
        mode = ActionBarMode.DEFAULT.name,
        search = "",
        sortProperty = Task.Property.MODIFIED_AT.name,
        sortAscending = false,
        visibleProperties = properties,
        searchableProperties = properties,
    )
    val actionBarState = ActionBarState(
        sortProperty = defaultFilterCriteria.sortProperty,
        sortAscending = defaultFilterCriteria.sortAscending,
        properties = propertyMap,
        visibleProperties = properties,
        searchableProperties = properties
    )

    entry<Screen.TaskList>(metadata = split()) {
        val router = LocalRouter.current
        val store = viewModel { TaskListScreenStore(router = router, state = TaskListScreenState(), taskUseCases = useCases.taskUseCases) }
        TaskListScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState.copy(title = "tasks", layout = ActionBarLayout.LIST),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.TaskPreferences,
                    defaults = defaultFilterCriteria,
                    entityType = EntityType.TASK,
                    onOkClick = { mode -> store.send(action = TaskListScreenAction.Ok(mode = mode)) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
    entry<Screen.TaskDetail>(metadata = split()) {
        val router = LocalRouter.current
        val splitScreen = LocalSplitScreen.current
        val store = viewModel { TaskDetailScreenStore(state = TaskDetailScreenState(), taskUseCases = useCases.taskUseCases, taskUuid = it.uuid) }
        TaskDetailScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = actionBarState.copy(title = "task", layout = if (splitScreen) ActionBarLayout.ALL else ActionBarLayout.DETAIL),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.TaskPreferences,
                    defaults = defaultFilterCriteria,
                    entityType = EntityType.TASK,
                    onOkClick = { mode -> store.send(action = TaskDetailScreenAction.Ok(mode = mode)) },
                    onCancelClick = { mode -> store.send(action = TaskDetailScreenAction.Cancel(mode = mode)) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
}
