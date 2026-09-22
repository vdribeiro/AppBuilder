package com.app.builder.ui.screen.tasklist

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.locale.toInstant
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.Task
import com.app.builder.domain.Task.Property
import com.app.builder.domain.gateway.task.TaskUseCases
import com.app.builder.plusOrMinus
import com.app.builder.toEnumOrNull
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
 * @property taskUseCases Use cases used to observe and delete tasks.
 */
class TaskListScreenStore(
    state: TaskListScreenState,
    private val router: Router,
    private val taskUseCases: TaskUseCases
): Store<TaskListScreenState, TaskListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: TaskListScreenState, action: TaskListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TaskListScreenAction.SelectTask -> selectTask(state = state, action = action)
            is TaskListScreenAction.Ok -> ok(state = state, action = action)
        }
    }

    /**
     * Observes tasks and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val tasksFlow = taskUseCases.observeTasks().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.TaskPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val criteriaFlow = stateFlow
            .map { it.toTaskListFilterCriteria() }
            .distinctUntilChanged()

        actionBarDataFlow
            .map { it.mode }
            .distinctUntilChanged()
            .observe(id = "syncMode") { newMode ->
                val mode = newMode.toEnumOrNull<ActionBarMode>() ?: return@observe

                if (mode == ActionBarMode.ADD) router.navigate(screen = Screen.TaskDetail(uuid = uuid().toString()), option = Router.NavOption.REPLACE_LAST)

                updateState { state ->
                    state.copy(
                        mode = mode,
                        selectedUuids = when {
                            state.mode != mode -> persistentListOf()
                            else -> state.selectedUuids
                        }
                    )
                }
            }

        val tasksActionBarFlow = combine(
            flow = tasksFlow,
            flow2 = actionBarDataFlow,
        ) { tasks, actionBarData ->
            tasks to actionBarData
        }.mapLatest { (tasks, actionBarData) ->
            val comparator = taskComparator(sortProperty = actionBarData.sortProperty, sortAscending = actionBarData.sortAscending)
            tasks
                .filter { it.matchesSearch(search = actionBarData.search, searchableProperties = actionBarData.searchableProperties) }
                .let { if (comparator == null) it else it.sortedWith(comparator = comparator) } to actionBarData
        }

        combine(
            flow = tasksActionBarFlow,
            flow2 = criteriaFlow
        ) { (tasks, actionBar), criteria ->
            tasks
                .map { it.toTaskItem(visibilityProperties = actionBar.visibleProperties, selectedUuids = criteria.selectedUuids) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterTasks") { tasks ->
                updateState { it.copy(tasks = tasks) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Toggles the selection of the tapped task when in delete mode, otherwise navigates to its detail screen.
     *
     * @param state Current task list state.
     * @param action Action carrying the UUID of the selected task.
     * @return The [Job] representing this execution.
     */
    private fun selectTask(state: TaskListScreenState, action: TaskListScreenAction.SelectTask): Job = launch(id = "selectTask") {
        when (state.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> router.navigate(screen = Screen.TaskDetail(uuid = action.taskUuid), option = Router.NavOption.REPLACE_LAST)

            ActionBarMode.BATCH_DELETE -> {
                val selectedUuid = action.taskUuid.toUuid()
                if (selectedUuid != null) {
                    val selectedUuids = state.selectedUuids.plusOrMinus(element = selectedUuid).toPersistentList()
                    updateState { it.copy(selectedUuids = selectedUuids) }
                }
            }
        }
    }

    /**
     * Commits the pending mode once the user confirms it.
     *
     * @param state Current task list state.
     * @param action Action carrying the mode being confirmed.
     * @return The [Job] representing this execution.
     */
    private fun ok(state: TaskListScreenState, action: TaskListScreenAction.Ok): Job = launch(id = "ok") {
        when (action.mode) {
            ActionBarMode.DEFAULT,
            ActionBarMode.SEARCH,
            ActionBarMode.ADD,
            ActionBarMode.EDIT,
            ActionBarMode.DELETE -> Unit

            ActionBarMode.BATCH_DELETE -> {
                val now = now()
                state.tasks.filter { it.uuid.toUuid() in state.selectedUuids }.forEach {
                    it.toTask(deletedAt = now)?.let { task -> taskUseCases.upsertTask(task = task) }
                }
            }

        }
    }

    /**
     * Extracts the [TaskListFilterCriteria] this state contributes to the displayed task list.
     *
     * @return The criteria derived from this state.
     */
    private fun TaskListScreenState.toTaskListFilterCriteria(): TaskListFilterCriteria = TaskListFilterCriteria(
        selectedUuids = selectedUuids
    )

    /**
     * Checks whether this task's searchable properties contain the given [search] text.
     *
     * @param search Search text to look for; an empty or blank value always matches.
     * @param searchableProperties Names of the [Property] values to search within.
     * @return `true` if any of the searchable properties contains [search], ignoring case.
     */
    private fun Task.matchesSearch(search: String, searchableProperties: List<String>): Boolean {
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
    private fun Task.toTaskItem(visibilityProperties: List<String>, selectedUuids: ImmutableList<Uuid>): TaskItem = TaskItem(
        uuid = uuid.toString(),
        selected = uuid in selectedUuids,
        modifiedAt = modifiedAt.takeIf { Property.MODIFIED_AT.name in visibilityProperties }?.toString(),
        deletedAt = deletedAt.takeIf { Property.DELETED_AT.name in visibilityProperties }?.toString(),
        title = title.takeIf { Property.TITLE.name in visibilityProperties },
        description = description.takeIf { Property.DESCRIPTION.name in visibilityProperties }
    )

    /**
     * Converts this item back into a [Task], stamped as deleted at [deletedAt].
     *
     * @param deletedAt Timestamp recorded as the task's deletion time.
     * @return The reconstructed [Task], or `null` if this item is missing its uuid, modified timestamp or state.
     */
    private fun TaskItem.toTask(deletedAt: Instant): Task? = Task(
        uuid = uuid.toUuid() ?: return null,
        modifiedAt = modifiedAt?.toInstant() ?: return null,
        deletedAt = deletedAt,
        title = title.orEmpty(),
        description = description.orEmpty(),
        state = state?.toEnumOrNull() ?: return null
    )

    companion object {
        private const val TAG = "TaskListScreenStore"
    }
}