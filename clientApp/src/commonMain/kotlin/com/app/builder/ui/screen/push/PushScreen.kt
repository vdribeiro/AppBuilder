package com.app.builder.ui.screen.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.component.Store
import com.app.builder.ui.component.bar.ActionBar
import com.app.builder.ui.component.button.PushPayloadType
import com.app.builder.ui.component.container.PushForm
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.useravatar.UserAvatar
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.screen.Screen

/**
 * Renders a form for composing and sending a push payload:
 * a type selector, a broadcast toggle, notification title/description fields (when the type is [PushPayloadType.NOTIFICATION]),
 * a user multi-selector (when not broadcasting), and a send button.
 *
 * @param navigationStore Drives the bottom navigation bar.
 * @param userAvatarStore The store for user avatar.
 * @param store Provides the push form state and receives its actions.
 */
@Composable
fun PushScreen(
    navigationStore: Store<NavigationState, Unit>,
    userAvatarStore: Store<UserAvatarState, Unit>,
    store: Store<PushScreenState, PushScreenAction>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    Screen(
        bottomBar = { Navigation(store = navigationStore) },
        topBar = { ActionBar(title = "notifications", avatar = { UserAvatar(store = userAvatarStore) }) }
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

@Preview
@Composable
private fun PushScreenPreview() = Preview {
    PushScreen(
        navigationStore = Store(initialState = NavigationState()),
        userAvatarStore = Store(initialState = UserAvatarState(userName = "Tails")),
        store = Store(initialState = PushScreenState()),
    )
}
