package com.app.builder.ui.navigation.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore

/**
 * The home routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.homeProvider(useCases: UseCases) {
    entry<Screen.Home> {
        HomeScreen(
            sessionStore = hostSessionStore(useCases = useCases),
        )
    }
}
