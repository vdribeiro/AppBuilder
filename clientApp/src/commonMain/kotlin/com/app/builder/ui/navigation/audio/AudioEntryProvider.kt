package com.app.builder.ui.navigation.audio

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The audio routes.
 *
 * @param sessionStore The session store shared by every screen.
 */
fun EntryProviderScope<NavKey>.audioProvider(sessionStore: Store<SessionState, Unit>) {
    entry<Screen.Audio> {
        AudioScreen(
            sessionStore = sessionStore,
        )
    }
}
