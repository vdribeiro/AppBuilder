package com.app.builder.ui.screen.configs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.ConfigList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Config Screen.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore The store driving the bottom navigation bar.
 * @param store The store driving the list of configs displayed in the screen.
 */
@Composable
fun ConfigsScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<ConfigScreenState, ConfigScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    Screen(
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) }
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
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Sonic")),
        navigationStore = Store(initialState = NavigationState()),
        store = Store(initialState = ConfigScreenState(configType = ConfigType.CLIENT_CONFIG))
    )
}
