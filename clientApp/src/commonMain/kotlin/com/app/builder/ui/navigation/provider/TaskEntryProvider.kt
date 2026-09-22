package com.app.builder.ui.navigation.provider

import kotlinx.collections.immutable.toImmutableMap
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
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split
import com.app.builder.ui.screen.taskdetail.TaskDetailScreen
import com.app.builder.ui.screen.taskdetail.TaskDetailScreenState
import com.app.builder.ui.screen.taskdetail.TaskDetailScreenStore
import com.app.builder.ui.screen.tasklist.TaskListScreen
import com.app.builder.ui.screen.tasklist.TaskListScreenState
import com.app.builder.ui.screen.tasklist.TaskListScreenStore

/**
 * The task routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.taskProvider(useCases: UseCases) {
    entry<Screen.TaskList>(metadata = split()) {
        val router = LocalRouter.current
        val splitScreen = LocalSplitScreen.current
        val layout = if (splitScreen) ActionBarLayout.ALL else ActionBarLayout.DETAIL
        TaskListScreen(
            actionBarStore = viewModel {
                ActionBarStore(
                    state = ActionBarState(title = "tasks"),
                    router = router,
                    authenticationUseCases = useCases.authenticationUseCases,
                    storageFile = AppFile.TaskPreferences,
                    defaults = defaults,
                    entityType = EntityType.TASK
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel {
                TaskListScreenStore(
                    router = router,
                    state = TaskListScreenState(
                        layout = ActionBarLayout.LIST,
                        properties = Task.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap(),
                    ),
                    taskUseCases = useCases.taskUseCases
                )
            }
        )
    }
    entry<Screen.TaskDetail>(metadata = split()) {
        val router = LocalRouter.current
//        val splitScreen = LocalSplitScreen.current
//        title = "task_detail", // TODO add id or name to title?
//        layout = if (splitScreen) ActionBarLayout.ALL else ActionBarLayout.DETAIL,
        TaskDetailScreen(
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { TaskDetailScreenStore(state = TaskDetailScreenState(), taskUseCases = useCases.taskUseCases, taskUuid = it.uuid) }
        )
    }
}
