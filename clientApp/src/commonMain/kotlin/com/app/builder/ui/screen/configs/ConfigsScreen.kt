package com.app.builder.ui.screen.configs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.ActionBar
import com.app.builder.ui.component.list.ConfigList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.useravatar.UserAvatar
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Config Screen.
 *
 * @param navigationStore The store driving the bottom navigation bar.
 * @param userAvatarStore The store for user avatar.
 * @param store The store driving the list of configs displayed in the screen.
 */
@Composable
fun ConfigsScreen(
    navigationStore: Store<NavigationState, Unit>,
    userAvatarStore: Store<UserAvatarState, Unit>,
    store: Store<ConfigScreenState, ConfigScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    Screen(
        bottomBar = { Navigation(store = navigationStore) },
        topBar = { ActionBar(title = state.configType.name.lowercase(), avatar = { UserAvatar(store = userAvatarStore) }) }
    ) {
        ConfigList(
            items = state.items,
            onValueChange = { item, value ->
                store.send(action = ConfigScreenAction.UpdateValue(item = item, value = value))
            }
        )
    }
}

@Preview
@Composable
private fun ConfigsScreenPreview() = Preview {
    ConfigsScreen(
        navigationStore = Store(initialState = NavigationState()),
        userAvatarStore = Store(initialState = UserAvatarState(userName = "Sonic")),
        store = Store(initialState = ConfigScreenState(configType = ConfigType.CLIENT_CONFIG))
    )
}
