package com.app.builder.ui.navigation.nfc

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore

/**
 * The nfc routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.nfcProvider(useCases: UseCases) {
    entry<Screen.NFC> {
        NfcScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            store = viewModel { NfcScreenStore(state = NfcScreenState(), authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
