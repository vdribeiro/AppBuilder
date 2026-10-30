package com.app.builder.ui.screen.registrylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.list.RegistryList
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The user-facing Registry List Screen, showing the list of registries.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the registry list state and receives its actions.
 */
@Composable
fun RegistryListScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
    store: Store<RegistryListScreenState, RegistryListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        RegistryList(
            items = state.registries,
            onClick = { store.send(action = RegistryListScreenAction.SelectRegistry(registryUuid = it.uuid)) }
        )
    }
}

@PreviewLightDark
@Composable
private fun RegistryListScreenPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "registry" to "Registry"
        )
    )
    RegistryListScreen(
        actionBarStore = previewStore(initialState = ActionBarState(avatarName = "Registry")),
        navigationStore = previewStore(initialState = NavigationState()),
        store = previewStore(initialState = RegistryListScreenState())
    )
}
