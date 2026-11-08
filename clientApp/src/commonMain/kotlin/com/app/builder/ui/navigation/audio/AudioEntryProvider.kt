package com.app.builder.ui.navigation.audio

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore

/**
 * The audio routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.audioProvider(useCases: UseCases) {
    entry<Screen.Audio> {
        AudioScreen(
            sessionStore = hostSessionStore(useCases = useCases),
        )
    }
}
