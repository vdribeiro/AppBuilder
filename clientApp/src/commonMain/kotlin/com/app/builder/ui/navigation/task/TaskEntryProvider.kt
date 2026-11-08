package com.app.builder.ui.navigation.task

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentList
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Task
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore
import com.app.builder.ui.navigation.scene.SplitSceneStrategy.Companion.split

/**
 * The task routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.taskProvider(useCases: UseCases) {
    entry<Screen.TaskList>(metadata = split()) {
        val router = LocalRouter.current
        TaskListScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            filtersStore = hostViewModel(key = FILTERS_KEY) { filtersStore() },
            store = viewModel { TaskListScreenStore(router = router, state = TaskListScreenState(), taskUseCases = useCases.taskUseCases) }
        )
    }
    entry<Screen.TaskDetail>(metadata = split()) {
        val router = LocalRouter.current
        TaskDetailScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            filtersStore = hostViewModel(key = FILTERS_KEY) { filtersStore() },
            store = viewModel { TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, taskUseCases = useCases.taskUseCases, taskUuid = it.uuid, new = it.new) }
        )
    }
}

/** The key of the task filters among the stores shared by the navigation host. */
private const val FILTERS_KEY = "task_filters"
/** The task properties by name. */
private val properties = Task.Property.entries.map { it.name }.toPersistentList()
/** The default task filters. */
private val defaults = AppFile.ActionBarData(
    search = "",
    searchableProperties = properties,
    sortAscending = false,
    sortProperty = Task.Property.MODIFIED_AT.name,
    visibleProperties = properties,
)

/**
 * Creates the task filters store.
 *
 * @return The task filters store.
 */
private fun filtersStore(): FiltersStore = FiltersStore(
    state = FiltersState(
        properties = Task.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap(),
        searchableProperties = properties,
        sortAscending = defaults.sortAscending,
        sortProperty = defaults.sortProperty,
        visibleProperties = properties
    ),
    storageFile = AppFile.TaskPreferences,
    defaults = defaults,
)
