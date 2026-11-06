package com.app.builder.ui.navigation.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.button.PushPayloadType
import com.app.builder.ui.component.container.PushForm
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
 * Renders a form for composing and sending a push payload:
 * a type selector, a broadcast toggle, notification title/description fields (when the type is [PushPayloadType.NOTIFICATION]),
 * a user multi-selector (when not broadcasting), and a send button.
 *
 * @param sessionStore The session store shared by every screen.
 * @param store Provides the push form state and receives its actions.
 */
@Composable
fun PushScreen(
    sessionStore: Store<SessionState, Unit>,
    store: Store<PushScreenState, PushScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(sessionStore = sessionStore) },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.NOTIFICATION) },
    ) {
        PushForm(
            type = state.type,
            onTypeSelected = { store.send(action = PushScreenAction.SelectType(type = it)) },
            broadcast = state.broadcast,
            onBroadcastChange = { store.send(action = PushScreenAction.ToggleBroadcast(value = it)) },
            title = state.notificationTitle,
            onTitleChange = { store.send(action = PushScreenAction.UpdateTitle(value = it)) },
            description = state.notificationDescription,
            onDescriptionChange = { store.send(action = PushScreenAction.UpdateDescription(value = it)) },
            users = state.users,
            selectedUsers = state.selectedUsers,
            onUserToggled = { store.send(action = PushScreenAction.ToggleUser(user = it)) },
            sending = state.sending,
            onSendClick = { store.send(action = PushScreenAction.Send) },
            sent = state.sent
        )
    }
}

@PreviewLightDark
@Composable
private fun PushScreenPreview() = Preview {
    PushScreen(
        sessionStore = previewStore(initialState = SessionState()),
        store = previewStore(initialState = PushScreenState()),
    )
}
