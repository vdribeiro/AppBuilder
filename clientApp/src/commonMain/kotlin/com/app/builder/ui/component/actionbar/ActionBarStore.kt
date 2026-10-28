package com.app.builder.ui.component.actionbar

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.resource.ImageResource
import com.app.builder.data.storage.AppFile
import com.app.builder.data.storage.StorageFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.status.StatusUseCases
import com.app.builder.plusOrMinus
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing an action bar, resolving the current user's avatar and write permission, switching between viewing and editing, following the application status and persisting its mode, sorting and property filters.
 *
 * @param state Initial action bar state.
 * @property router The router used for navigation.
 * @property authenticationUseCases Use cases used to observe the current user.
 * @property statusUseCases Use cases used to observe the application status, or `null` to never show it.
 * @property storageFile File the action bar data is persisted to, or `null` to keep it in memory only.
 * @property defaults Action bar data used when [storageFile] holds nothing yet.
 * @property entityType Entity the write permission is checked against, or `null` when the screen has no entity.
 * @property onAddClick Called when the add button is clicked.
 * @property onEditClick Called when the edit button is clicked.
 * @property onSaveClick Called when the save button is clicked.
 * @property onCancelClick Called when the cancel button is clicked, or when the write permission is lost while editing.
 * @property onDeleteClick Called when the delete button is clicked.
 */
class ActionBarStore(
    state: ActionBarState,
    private val router: Router,
    private val authenticationUseCases: AuthenticationUseCases,
    private val statusUseCases: StatusUseCases? = null,
    private val storageFile: StorageFile<AppFile.ActionBarData>? = null,
    private val defaults: AppFile.ActionBarData = AppFile.ActionBarData(),
    private val entityType: EntityType? = null,
    private val onAddClick: (suspend () -> Unit)? = null,
    private val onEditClick: (suspend () -> Unit)? = null,
    private val onSaveClick: (suspend () -> Unit)? = null,
    private val onCancelClick: (suspend () -> Unit)? = null,
    private val onDeleteClick: (suspend () -> Unit)? = null,
): Store<ActionBarState, ActionBarAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: ActionBarState, action: ActionBarAction) {
        super.reducer(state, action)
        when (action) {
            ActionBarAction.OpenUser -> router.navigate(screen = Screen.UserProfile)
            ActionBarAction.Add -> add()
            ActionBarAction.Edit -> edit()
            ActionBarAction.Save -> save()
            ActionBarAction.Cancel -> cancel()
            ActionBarAction.Delete -> delete()
            ActionBarAction.Login -> router.navigate(screen = Screen.Login)
            is ActionBarAction.Search -> search(action = action)
            is ActionBarAction.ToggleSearchableProperty -> toggleSearchableProperty(state = state, action = action)
            is ActionBarAction.SelectSortOrder -> selectSortOrder(action = action)
            is ActionBarAction.SelectSortProperty -> selectSortProperty(action = action)
            is ActionBarAction.ToggleVisibleProperty -> toggleVisibleProperty(state = state, action = action)
        }
    }

    /**
     * Loads the persisted action bar data, keeps the avatar and write permission in sync with the current user and follows the application status.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        // Assert the file exists and holds every value
        storageFile?.run {
            val persisted = load()
            val data = persisted.withDefaults()
            if (data != persisted) save { data }

            updateState {
                it.copy(
                    searchableProperties = data.searchableProperties.toPersistentList(),
                    sortAscending = data.sortAscending,
                    sortProperty = data.sortProperty,
                    visibleProperties = data.visibleProperties.toPersistentList()
                )
            }
        }

        authenticationUseCases.observeCurrentUser().observe(id = "current_user") { user ->
            val userImage = when (user) {
                null -> ImageResource.Kotlin.toImage()
                else -> user.avatar?.let { avatar -> Image(url = avatar) }
            }
            val write = entityType?.let { user?.hasPermission(entityType = it, permission = Permission.WRITE) } ?: false
            val editingLost = state.editing && !write

            updateState {
                it.copy(
                    avatarName = user?.name,
                    avatarImage = userImage,
                    showLogin = user == null,
                ).withButtons(writable = write, editing = it.editing && write)
            }
            if (editingLost) onCancelClick?.invoke()
        }

        statusUseCases?.observeSynced()?.observe(id = "synced") { synced ->
            updateState { it.copy(synced = synced) }
        }
        statusUseCases?.observeClockTrusted()?.observe(id = "clock_trusted") { clockTrusted ->
            updateState { it.copy(clockTrusted = clockTrusted) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Backfills every value this payload is missing with the [defaults], so a payload persisted before a value existed does not leave the list unsorted or unsearchable.
     *
     * @return The persisted payload completed with the [defaults], or the [defaults] when nothing is persisted yet.
     */
    private fun AppFile.ActionBarData?.withDefaults(): AppFile.ActionBarData = when (this) {
        null -> defaults
        else -> copy(
            searchableProperties = searchableProperties.ifEmpty { defaults.searchableProperties },
            sortProperty = sortProperty.ifBlank { defaults.sortProperty },
            visibleProperties = visibleProperties.ifEmpty { defaults.visibleProperties }
        )
    }

    /**
     * Resolves which buttons are shown for the given write permission and mode.
     * Editing shows only save and cancel, viewing shows add, edit and delete.
     *
     * @param writable Whether the current user has write permission on the entity.
     * @param editing Whether the current domain is being edited.
     * @return This state with the permission, the mode and the buttons updated.
     */
    private fun ActionBarState.withButtons(writable: Boolean = this.writable, editing: Boolean = this.editing): ActionBarState = copy(
        writable = writable,
        editing = editing,
        showAdd = onAddClick != null && writable && !editing,
        showEdit = onEditClick != null && writable && !editing,
        showSave = onSaveClick != null && writable && editing,
        showCancel = onCancelClick != null && writable && editing,
        showDelete = onDeleteClick != null && writable && !editing,
    )

    /**
     * Call add callback.
     *
     * @return The [Job] representing this execution.
     */
    private fun add(): Job = launch(id = "add") {
        onAddClick?.invoke()
    }

    /**
     * Switches to editing and calls the edit callback.
     *
     * @return The [Job] representing this execution.
     */
    private fun edit(): Job = launch(id = "edit") {
        updateState { it.withButtons(editing = true) }
        onEditClick?.invoke()
    }

    /**
     * Calls the save callback and switches back to viewing.
     *
     * @return The [Job] representing this execution.
     */
    private fun save(): Job = launch(id = "save") {
        onSaveClick?.invoke()
        updateState { it.withButtons(editing = false) }
    }

    /**
     * Switches back to viewing and calls the cancel callback.
     *
     * @return The [Job] representing this execution.
     */
    private fun cancel(): Job = launch(id = "cancel") {
        updateState { it.withButtons(editing = false) }
        onCancelClick?.invoke()
    }

    /**
     * Call delete callback.
     *
     * @return The [Job] representing this execution.
     */
    private fun delete(): Job = launch(id = "delete") {
        onDeleteClick?.invoke()
    }

    /**
     * Persists the current search query.
     *
     * @param action Action carrying the new search query text.
     * @return The [Job] representing this execution.
     */
    private fun search(action: ActionBarAction.Search): Job = launch(id = "search") {
        storageFile?.save { (it ?: defaults).copy(search = action.search) }
    }

    /**
     * Adds or removes a property from the searchable ones, then persists the result.
     *
     * @param state Current action bar state.
     * @param action Action carrying the key of the property to toggle.
     * @return The [Job] representing this execution.
     */
    private fun toggleSearchableProperty(state: ActionBarState, action: ActionBarAction.ToggleSearchableProperty): Job = launch(id = "toggleSearchableProperty") {
        val searchableProperties = state.searchableProperties.plusOrMinus(element = action.property).toPersistentList()
        updateState { it.copy(searchableProperties = searchableProperties) }
        storageFile?.save { (it ?: defaults).copy(searchableProperties = searchableProperties) }
    }

    /**
     * Applies and persists the direction the list is sorted in.
     *
     * @param action Action carrying the new sort direction.
     * @return The [Job] representing this execution.
     */
    private fun selectSortOrder(action: ActionBarAction.SelectSortOrder): Job = launch(id = "selectSortOrder") {
        updateState { it.copy(sortAscending = action.ascending) }
        storageFile?.save { (it ?: defaults).copy(sortAscending = action.ascending) }
    }

    /**
     * Applies and persists the property the list is sorted by.
     *
     * @param action Action carrying the key of the property to sort by.
     * @return The [Job] representing this execution.
     */
    private fun selectSortProperty(action: ActionBarAction.SelectSortProperty): Job = launch(id = "selectSortProperty") {
        updateState { it.copy(sortProperty = action.property) }
        storageFile?.save { (it ?: defaults).copy(sortProperty = action.property) }
    }

    /**
     * Adds or removes a property from the visible ones, then persists the result.
     *
     * @param state Current action bar state.
     * @param action Action carrying the key of the property to toggle.
     * @return The [Job] representing this execution.
     */
    private fun toggleVisibleProperty(state: ActionBarState, action: ActionBarAction.ToggleVisibleProperty): Job = launch(id = "toggleVisibleProperty") {
        val visibilityProperties = state.visibleProperties.plusOrMinus(element = action.property).toPersistentList()
        updateState { it.copy(visibleProperties = visibilityProperties) }
        storageFile?.save { (it ?: defaults).copy(visibleProperties = visibilityProperties) }
    }

    companion object {
        private const val TAG = "ActionBarStore"
    }
}
