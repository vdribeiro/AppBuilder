package com.app.builder.ui.navigation.configs

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.Screen

/**
 * The flags and configs route.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.configsProvider(useCases: UseCases) {
    entry<Screen.Configs> {
        ConfigsScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
            store = viewModel { ConfigScreenStore(state = ConfigScreenState(), configUseCases = useCases.configUseCases, authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
