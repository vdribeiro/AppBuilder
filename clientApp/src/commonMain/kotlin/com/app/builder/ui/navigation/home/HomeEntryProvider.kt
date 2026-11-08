package com.app.builder.ui.navigation.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.lifecycle.hostViewModel
import com.app.builder.ui.navigation.Screen

/**
 * The home routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.homeProvider(useCases: UseCases) {
    entry<Screen.Home> {
        HomeScreen(
            sessionStore = hostViewModel(key = SessionStore.KEY) { SessionStore.get(useCases = useCases) },
        )
    }
}
