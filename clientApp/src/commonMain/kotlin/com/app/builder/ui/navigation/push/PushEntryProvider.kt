package com.app.builder.ui.navigation.push

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore

/**
 * The push notifications route.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.pushProvider(useCases: UseCases) {
    entry<Screen.Push> {
        PushScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            store = viewModel { PushScreenStore(state = PushScreenState(), pushUseCases = useCases.pushUseCases, userUseCases = useCases.userUseCases) },
        )
    }
}
