package com.app.builder.ui.navigation.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.list.UserList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing User List Screen, showing the list of users.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The user filters shared by every user screen.
 * @param store Provides the user list state and receives its actions.
 */
@Composable
fun UserListScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<UserListScreenState, UserListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = {
            ActionBar(
                sessionStore = sessionStore,
                filtersStore = filtersStore,
                entityType = EntityType.USER,
                onAddClick = { router.navigate(screen = Screen.UserDetail(uuid = uuid().toString(), new = true), option = Router.NavOption.REPLACE_LAST) },
            )
        },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.USER) },
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
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(initialState = UserListScreenState())
    )
}
