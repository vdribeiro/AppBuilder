package com.app.builder.ui.navigation.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.filters.FiltersAction
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.list.RegistryList
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
 * The user-facing Registry List Screen, showing the list of registries.
 *
 * @param sessionStore The session store shared by every screen.
 * @param filtersStore The registry filters shared by every registry screen.
 * @param store Provides the registry list state and receives its actions.
 */
@Composable
fun RegistryListScreen(
    sessionStore: Store<SessionState, Unit>,
    filtersStore: Store<FiltersState, FiltersAction>,
    store: Store<RegistryListScreenState, RegistryListScreenAction>
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()

    val router = LocalRouter.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = { ActionBar(sessionStore = sessionStore, filtersStore = filtersStore) },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.REGISTRY) },
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
        sessionStore = previewStore(initialState = SessionState()),
        filtersStore = previewStore(initialState = FiltersState()),
        store = previewStore(initialState = RegistryListScreenState())
    )
}
