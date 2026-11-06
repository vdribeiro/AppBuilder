package com.app.builder.ui.navigation.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.UserList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing User List Screen, showing the list of users.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the user list state and receives its actions.
 */
@Composable
fun UserListScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<UserListScreenState, UserListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore, selected = NavigationRoute.USER) },
    ) {
        UserList(
            items = state.users,
            onClick = { store.send(action = UserListScreenAction.SelectUser(userUuid = it.uuid)) }
        )
    }
}

@PreviewLightDark
@Composable
private fun UserListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "user_name" to "Name"
        )
    )
    UserListScreen(
        actionBarStore = previewStore(initialState = ActionBarState(avatarName = "Users")),
        navigationStore = previewStore(initialState = NavigationState()),
        store = previewStore(initialState = UserListScreenState())
    )
}
