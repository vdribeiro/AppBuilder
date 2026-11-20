package com.app.builder.ui.navigation.user

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import androidx.compose.runtime.snapshotFlow
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.ui.component.user.filtered
import com.app.builder.ui.component.user.toUserItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * Store backing the user list, combining users with the shared action bar state to filter, sort, and select them.
 *
 * @param state Initial user list state.
 * @property router The router used for navigation.
 * @property userUseCases Use cases used to observe users.
 */
class UserListScreenStore(
    state: UserListScreenState,
    private val router: Router,
    private val userUseCases: UserUseCases
): Store<UserListScreenState, UserListScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: UserListScreenState, action: UserListScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is UserListScreenAction.SelectUser -> selectUser(action = action)
        }
    }

    /**
     * Observes users and keeps the displayed list in sync.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val usersFlow = userUseCases.observeUsers().toStateFlow(initialValue = emptyList())
        val actionBarDataFlow = AppFile.UserPreferences.cache()
            .mapNotNull { it }
            .distinctUntilChanged()
        val selectedUuidFlow = snapshotFlow { (router.backStack.lastOrNull { it is Screen.UserDetail } as? Screen.UserDetail)?.uuid }
            .distinctUntilChanged()

        val usersActionBarFlow = combine(
            flow = usersFlow,
            flow2 = actionBarDataFlow,
        ) { users, actionBarData ->
            users to actionBarData
        }.mapLatest { (users, actionBarData) ->
            users.filtered(actionBarData = actionBarData) to actionBarData
        }

        combine(
            flow = usersActionBarFlow,
            flow2 = selectedUuidFlow
        ) { (users, actionBar), selectedUuid ->
            users
                .map { it.toUserItem(visibilityProperties = actionBar.visibleProperties, selectedUuid = selectedUuid) }
                .toPersistentList()
        }
            .flowOn(context = Dispatcher.Default)
            .observe(id = "filterUsers") { users ->
                updateState { it.copy(users = users) }
            }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Navigates to the detail screen of the selected user.
     *
     * @param action Action carrying the UUID of the selected user.
     * @return The [Job] representing this execution.
     */
    private fun selectUser(action: UserListScreenAction.SelectUser): Job = launch(id = "selectUser") {
        router.navigate(screen = Screen.UserDetail(uuid = action.userUuid), option = Router.NavOption.REPLACE_LAST)
    }

    companion object {
        private const val TAG = "UserListScreenStore"
    }
}
