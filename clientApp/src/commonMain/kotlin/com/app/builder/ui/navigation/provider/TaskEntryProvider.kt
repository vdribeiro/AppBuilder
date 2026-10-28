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
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split
import com.app.builder.ui.screen.taskdetail.TaskDetailScreen
import com.app.builder.ui.screen.taskdetail.TaskDetailScreenAction
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
        val store = viewModel { TaskListScreenStore(router = router, state = TaskListScreenState(), taskUseCases = useCases.taskUseCases) }
        TaskListScreen(
            actionBarStore = viewModel {
                getActionBarStore(
                    router = router,
                    useCases = useCases,
                    showAdd = true,
                    onSaveClick = null,
                    onDeleteClick = null
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
    entry<Screen.TaskDetail>(metadata = split()) {
        val router = LocalRouter.current
        val splitScreen = LocalSplitScreen.current
        val store = viewModel { TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, authenticationUseCases = useCases.authenticationUseCases, taskUseCases = useCases.taskUseCases, taskUuid = it.uuid) }
        TaskDetailScreen(
            actionBarStore = viewModel {
                getActionBarStore(
                    router = router,
                    useCases = useCases,
                    showAdd = splitScreen,
                    onSaveClick = { store.send(action = TaskDetailScreenAction.Save) },
                    onDeleteClick = { store.send(action = TaskDetailScreenAction.Delete) }
                )
            },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.TASK), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = store
        )
    }
}

/** The task properties by name. */
private val properties = Task.Property.entries.map { it.name }.toPersistentList()
/** The task property map of name and translation key. */
private val propertyMap = Task.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()
/** The default action bar data. */
private val defaults = AppFile.ActionBarData(
    search = "",
    searchableProperties = properties,
    sortAscending = false,
    sortProperty = Task.Property.MODIFIED_AT.name,
    visibleProperties = properties,
)
/** The default action bar state. */
private val actionBarState = ActionBarState(
    properties = propertyMap,
    searchableProperties = properties,
    sortAscending = defaults.sortAscending,
    sortProperty = defaults.sortProperty,
    visibleProperties = properties
)

/**
 * Get the task action bar store.
 *
 * @param router The router.
 * @param useCases The use cases.
 * @param showAdd true to show add button, false otherwise.
 * @param onSaveClick The save click callback, or null to hide the save action.
 * @param onDeleteClick The delete click callback, or null to hide the delete action.
 * @return The action bar store.
 */
private fun getActionBarStore(
    router: Router,
    useCases: UseCases,
    showAdd: Boolean,
    onSaveClick: (suspend () -> Unit)?,
    onDeleteClick: (suspend () -> Unit)?
): ActionBarStore = ActionBarStore(
    state = actionBarState,
    router = router,
    authenticationUseCases = useCases.authenticationUseCases,
    statusUseCases = useCases.statusUseCases,
    storageFile = AppFile.TaskPreferences,
    defaults = defaults,
    entityType = EntityType.TASK,
    onAddClick = if (showAdd) {
        {
            val task = Task()
            useCases.taskUseCases.upsertTask(task = task)
            router.navigate(screen = Screen.TaskDetail(uuid = task.uuid.toString()), option = Router.NavOption.REPLACE_LAST)
        }
    } else null,
    onSaveClick = onSaveClick,
    onDeleteClick = onDeleteClick
)
