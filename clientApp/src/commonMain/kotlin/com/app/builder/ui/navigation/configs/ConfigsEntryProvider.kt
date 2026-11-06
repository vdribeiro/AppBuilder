package com.app.builder.ui.navigation.configs

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The flags and configs route.
 *
 * @param useCases The use cases.
 * @param sessionStore The session store shared by every screen.
 */
fun EntryProviderScope<NavKey>.configsProvider(useCases: UseCases, sessionStore: Store<SessionState, Unit>) {
    entry<Screen.Configs> {
        ConfigsScreen(
            sessionStore = sessionStore,
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(), configUseCases = useCases.configUseCases, authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
