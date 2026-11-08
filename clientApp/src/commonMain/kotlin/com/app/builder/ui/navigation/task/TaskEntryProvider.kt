package com.app.builder.ui.navigation.task

import kotlinx.collections.immutable.toImmutableMap
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Task
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
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
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { TaskListScreenStore(router = router, state = TaskListScreenState(), taskUseCases = useCases.taskUseCases) }
        )
    }
    entry<Screen.TaskDetail>(metadata = split()) {
        val router = LocalRouter.current
        TaskDetailScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            filtersStore = filtersStore(),
            store = viewModel { TaskDetailScreenStore(state = TaskDetailScreenState(), router = router, taskUseCases = useCases.taskUseCases, taskUuid = it.uuid, new = it.new) }
        )
    }
}

/** The task properties by key, labelled by their translation keys. */
private val properties = Task.Property.entries.associate { entry -> entry.name to entry.translationKey }.toImmutableMap()

/**
 * Gets the task filters store shared by the navigation host, creating it on first use.
 *
 * @return The task filters store.
 */
@Composable
private fun filtersStore(): FiltersStore = hostViewModel(key = FiltersStore.key(storageFile = AppFile.TaskPreferences)) { FiltersStore.get(properties = properties, storageFile = AppFile.TaskPreferences, sortProperty = Task.Property.MODIFIED_AT.name, sortAscending = false) }
