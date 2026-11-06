package com.app.builder.ui.navigation.push

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The push notifications route.
 *
 * @param useCases The use cases.
 * @param sessionStore The session store shared by every screen.
 */
fun EntryProviderScope<NavKey>.pushProvider(useCases: UseCases, sessionStore: Store<SessionState, Unit>) {
    entry<Screen.Push> {
        PushScreen(
            sessionStore = sessionStore,
            store = viewModel { PushScreenStore(state = PushScreenState(), pushUseCases = useCases.pushUseCases, userUseCases = useCases.userUseCases) },
        )
    }
}
