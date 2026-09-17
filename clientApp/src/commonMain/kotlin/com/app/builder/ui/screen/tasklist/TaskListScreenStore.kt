package com.app.builder.ui.screen.tasklist

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.Task
import com.app.builder.domain.Task.Property
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.plusOrMinus
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.list.TaskItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the task list, combining tasks with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial task list state.
 * @property router Router used to navigate to a task's detail screen.
 * @property authenticationUseCases Use cases used to observe the current user.
 * @property taskUseCases Use cases used to observe and delete tasks.
 */
class TaskListScreenStore(
    state: TaskListScreenState,
    private val router: Router,
    private val authenticationUseCases: AuthenticationUseCases,
    private val taskUseCases: TaskUseCases
): Store<TaskListScreenState, TaskListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: TaskListScreenState, action: TaskListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TaskListScreenAction.SelectTask -> selectTask(state = state, action = action)
            is TaskListScreenAction.ModeChange -> changeMode(action = action)
            is TaskListScreenAction.Search -> search(action = action)
            is TaskListScreenAction.Ok -> {}
            is TaskListScreenAction.Cancel -> {}
            is TaskListScreenAction.SelectSortProperty -> selectSortProperty(action = action)
            is TaskListScreenAction.SelectSortOrder -> selectSortOrder(action = action)
            is TaskListScreenAction.VisibleProperties -> setVisibleProperties(action = action)
            is TaskListScreenAction.SearchableProperties -> setSearchableProperties(action = action)
        }
    }

    /** Observes tasks and keeps the displayed list in sync. */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        AppFile.TaskPreferences.load()?.let { taskPreferences -> updateState { it.copy(filterCriteria = taskPreferences) } } ?: save { it }

        authenticationUseCases.observeCurrentUser().observe(id = "current_user") { user ->
            val write = user?.hasPermission(entityType = EntityType.TASK, permission = Permission.WRITE) ?: false
            updateState { it.copy(write = write) }
        }

        val tasksFlow = taskUseCases.observeTasks()
        val criteriaFlow = stateFlow
            .map { it.filterCriteria }
            .distinctUntilChanged()

        combine(
            flow = criteriaFlow,
            flow2 = tasksFlow,
        ) { criteria, tasks ->
            val comparator = taskComparator(sortProperty = criteria.sortProperty, sortAscending = criteria.sortAscending)
            tasks
                .filter { it.matchesSearch(search = criteria.search, searchableProperties = criteria.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) }
                .map { it.toTaskItem(visibilityProperties = criteria.visibleProperties, selectedUuids = criteria.selectedUuids) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterTasks") { tasks ->
                updateState { it.copy(tasks = tasks) }
            }

        taskUseCases.upsertTask(
            task = Task(
                uuid = uuid(),
                modifiedAt = now(),
                deletedAt = null,
                title = "Task Title",
                description = "Task Description",
                state = Task.State.TODO
            )
        )

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Toggles the selection of the tapped task when in delete mode, otherwise navigates to its detail screen.
     *
     * @param state Current task list state.
     * @param action Action carrying the UUID of the selected task.
     */
    private fun selectTask(state: TaskListScreenState, action: TaskListScreenAction.SelectTask): Job = launch(id = "selectTask") {
        when (state.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> router.navigate(screen = Screen.TaskDetail(uuid = action.taskUuid), option = Router.NavOption.REPLACE_LAST)

            ActionBarMode.BATCH_DELETE -> {
                val selectedUuids = state.selectedUuids.plusOrMinus(element = action.taskUuid).toPersistentList()
                updateState { it.copy(selectedUuids = selectedUuids) }
            }
        }
    }

    /**
     * Switches the action bar to [action]'s new mode.
     *
     * @param action Action carrying the new [ActionBarMode].
     */
    private fun changeMode(action: TaskListScreenAction.ModeChange): Job = launch(id = "changeMode") {
        updateState { it.copy(mode = action.new, selectedUuids = persistentListOf()) }
    }

    /**
     * Updates and persists the current search text.
     *
     * @param action Action carrying the new search text.
     */
    private fun search(action: TaskListScreenAction.Search): Job = launch(id = "search") {
        updateState { it.copy(filterCriteria = it.filterCriteria.copy(search = action.search)) }
        save { it.copy(search = action.search) }
    }

    /**
     * Updates and persists the property tasks are sorted by.
     *
     * @param action Action carrying the newly selected [Property] name.
     */
    private fun selectSortProperty(action: TaskListScreenAction.SelectSortProperty): Job = launch(id = "selectSortProperty") {
        updateState { it.copy(filterCriteria = it.filterCriteria.copy(sortProperty = action.property)) }
        save { it.copy(sortProperty = action.property) }
    }

    /**
     * Updates and persists the sort order.
     *
     * @param action Action carrying whether sorting should be ascending.
     */
    private fun selectSortOrder(action: TaskListScreenAction.SelectSortOrder): Job = launch(id = "selectSortOrder") {
        updateState { it.copy(filterCriteria = it.filterCriteria.copy(sortAscending = action.ascending)) }
        save { it.copy(sortAscending = action.ascending) }
    }

    /**
     * Updates and persists which properties are visible on task items.
     *
     * @param action Action carrying the newly selected [Property] names.
     */
    private fun setVisibleProperties(action: TaskListScreenAction.VisibleProperties): Job = launch(id = "setVisibleProperties") {
        updateState { it.copy(filterCriteria = it.filterCriteria.copy(visibleProperties = action.properties)) }
        save { it.copy(visibleProperties = action.properties) }
    }

    /**
     * Updates and persists which properties are searchable.
     *
     * @param action Action carrying the newly selected [Property] names.
     */
    private fun setSearchableProperties(action: TaskListScreenAction.SearchableProperties): Job = launch(id = "setSearchableProperties") {
        updateState { it.copy(filterCriteria = it.filterCriteria.copy(searchableProperties = action.properties)) }
        save { it.copy(searchableProperties = action.properties) }
    }

    /**
     * Persists the task list filter preferences, applying [function] to the previously saved criteria.
     *
     * @param function Transform applied to the current (or default) [AppFile.FilterCriteria] before saving.
     */
    private suspend fun save(function: (AppFile.FilterCriteria) -> AppFile.FilterCriteria) {
        AppFile.TaskPreferences.save { function(it ?: defaultFilterCriteria) }
    }

    /**
     * Checks whether this task's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Task.matchesSearch(search: String, searchableProperties: ImmutableList<String>): Boolean {
        if (search.isBlank()) return true
        return searchableProperties.any { property ->
            when (property) {
                Property.TITLE.name -> title
                Property.DESCRIPTION.name -> description
                Property.MODIFIED_AT.name -> modifiedAt.toString()
                Property.DELETED_AT.name -> deletedAt?.toString()
                else -> null
            }?.contains(other = search, ignoreCase = true) == true
        }
    }

    /**
     * Builds a comparator for the given [sortProperty], reversed when [sortAscending] is `false`.
     *
     * @param sortProperty Name of the [Task.Property] to sort by.
     * @param sortAscending Whether the comparator should sort in ascending order.
     * @return Comparator for the given property, or `null` if it doesn't match a known [Task.Property].
     */
    private fun taskComparator(sortProperty: String, sortAscending: Boolean): Comparator<Task>? {
        val comparator = when (sortProperty) {
            Property.MODIFIED_AT.name -> compareBy<Task> { it.modifiedAt }
            Property.DELETED_AT.name -> compareBy { it.deletedAt }
            Property.TITLE.name -> compareBy { it.title }
            Property.DESCRIPTION.name -> compareBy { it.description }
            else -> return null
        }
        return if (sortAscending) comparator else comparator.reversed()
    }

    /**
     * Converts this task into a [TaskItem], including only the properties named in [visibilityProperties].
     *
     * @param visibilityProperties Names of the [Property] values that should be visible on the item.
     * @param selectedUuids UUIDs of the tasks the user has picked.
     * @return Task item with hidden properties set to `null`.
     */
    private fun Task.toTaskItem(visibilityProperties: ImmutableList<String>, selectedUuids: ImmutableList<String>): TaskItem = TaskItem(
        uuid = uuid.toString(),
        selected = uuid.toString() in selectedUuids,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.toString(),
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.toString(),
        title = title.takeIf { Property.TITLE.name in visibilityProperties },
        description = description.takeIf { Property.DESCRIPTION.name in visibilityProperties }
    )

    companion object {
        private const val TAG = "TaskListScreenStore"
    }
}