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
import com.app.builder.ui.component.bar.ActionBarMode
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

class ActionBarStore(
    state: ActionBarState,
    private val router: Router,
    private val authenticationUseCases: AuthenticationUseCases,
    private val storageFile: StorageFile<AppFile.ActionBarData>? = null,
    private val defaults: AppFile.ActionBarData = AppFile.ActionBarData(),
    private val entityType: EntityType? = null,
    private val onBackClick: (() -> Unit)? = null,
    private val onOkClick: (ActionBarMode) -> Unit = {},
    private val onCancelClick: (ActionBarMode) -> Unit = {},
): Store<ActionBarState, ActionBarAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: ActionBarState, action: ActionBarAction) {
        super.reducer(state, action)
        when (action) {
            ActionBarAction.Back -> onBackClick?.invoke()
            ActionBarAction.Login -> router.navigate(screen = Screen.Login)
            ActionBarAction.OpenUser -> router.navigate(screen = Screen.UserProfile)
            is ActionBarAction.ModeChange -> changeMode(action = action)
            is ActionBarAction.Search -> search(action = action)
            is ActionBarAction.SelectSortProperty -> selectSortProperty(action = action)
            is ActionBarAction.SelectSortOrder -> selectSortOrder(action = action)
            is ActionBarAction.ToggleVisibleProperty -> toggleVisibleProperty(state = state, action = action)
            is ActionBarAction.ToggleSearchableProperty -> toggleSearchableProperty(state = state, action = action)
            is ActionBarAction.Ok -> onOkClick(action.mode)
            is ActionBarAction.Cancel -> onCancelClick(action.mode)
        }
    }

    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        storageFile?.load() ?: storageFile?.save { defaults }

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
                    write = write
                )
            }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    private fun changeMode(action: ActionBarAction.ModeChange): Job = launch(id = "changeMode") {
        storageFile?.save { (it ?: defaults).copy(mode = action.mode.name) }
    }

    private fun search(action: ActionBarAction.Search): Job = launch(id = "search") {
        storageFile?.save { (it ?: defaults).copy(search = action.search) }
    }

    private fun selectSortProperty(action: ActionBarAction.SelectSortProperty): Job = launch(id = "selectSortProperty") {
        updateState { it.copy(sortProperty = action.property) }
        storageFile?.save { (it ?: defaults).copy(sortProperty = action.property) }
    }

    private fun selectSortOrder(action: ActionBarAction.SelectSortOrder): Job = launch(id = "selectSortOrder") {
        updateState { it.copy(sortAscending = action.ascending) }
        storageFile?.save { (it ?: defaults).copy(sortAscending = action.ascending) }
    }

    private fun toggleVisibleProperty(state: ActionBarState, action: ActionBarAction.ToggleVisibleProperty): Job = launch(id = "toggleVisibleProperty") {
        val visibilityProperties = state.visibleProperties.plusOrMinus(element = action.property).toPersistentList()
        updateState { it.copy(visibleProperties = visibilityProperties) }
        storageFile?.save { (it ?: defaults).copy(visibleProperties = visibilityProperties) }
    }

    private fun toggleSearchableProperty(state: ActionBarState, action: ActionBarAction.ToggleSearchableProperty): Job = launch(id = "toggleSearchableProperty") {
        val searchableProperties = state.searchableProperties.plusOrMinus(element = action.property).toPersistentList()
        updateState { it.copy(searchableProperties = searchableProperties) }
        storageFile?.save { (it ?: defaults).copy(searchableProperties = searchableProperties) }
    }

    companion object {
        private const val TAG = "ActionBarStore"
    }
}
