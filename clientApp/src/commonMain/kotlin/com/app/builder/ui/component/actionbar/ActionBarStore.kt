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
import com.app.builder.plusOrMinus
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing an action bar, resolving the current user's avatar and write permission and persisting its mode, sorting and property filters.
 *
 * @param state Initial action bar state.
 * @property router The router used for navigation.
 * @property authenticationUseCases Use cases used to observe the current user.
 * @property storageFile File the action bar data is persisted to, or `null` to keep it in memory only.
 * @property defaults Action bar data used when [storageFile] holds nothing yet.
 * @property entityType Entity the write permission is checked against, or `null` when the screen has no entity.
 * @property onAddClick Called when the add button is clicked.
 * @property onDeleteClick Called when the delete button is clicked.
 */
class ActionBarStore(
    state: ActionBarState,
    private val router: Router,
    private val authenticationUseCases: AuthenticationUseCases,
    private val storageFile: StorageFile<AppFile.ActionBarData>? = null,
    private val defaults: AppFile.ActionBarData = AppFile.ActionBarData(),
    private val entityType: EntityType? = null,
    private val onAddClick: (suspend () -> Unit)? = null,
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
     * Loads the persisted action bar data and keeps the avatar and write permission in sync with the current user.
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

            updateState {
                it.copy(
                    avatarName = user?.name,
                    avatarImage = userImage,
                    showAdd = onAddClick != null && write,
                    showDelete = onDeleteClick != null && write,
                    showLogin = user == null,
                )
            }
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
     * Call add callback.
     *
     * @return The [Job] representing this execution.
     */
    private fun add(): Job = launch(id = "add") {
        onAddClick?.invoke()
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
