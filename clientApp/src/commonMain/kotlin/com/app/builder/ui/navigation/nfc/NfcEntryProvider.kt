package com.app.builder.ui.navigation.nfc

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The nfc routes.
 *
 * @param useCases The use cases.
 * @param sessionStore The session store shared by every screen.
 */
fun EntryProviderScope<NavKey>.nfcProvider(useCases: UseCases, sessionStore: Store<SessionState, Unit>) {
    entry<Screen.NFC> {
        NfcScreen(
            sessionStore = sessionStore,
            store = viewModel { NfcScreenStore(state = NfcScreenState(), authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
