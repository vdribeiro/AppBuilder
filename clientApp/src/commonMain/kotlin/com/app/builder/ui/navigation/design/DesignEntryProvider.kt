package com.app.builder.ui.navigation.design

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The design section routes.
 *
 * @param sessionStore The session store shared by every screen.
 */
fun EntryProviderScope<NavKey>.designProvider(sessionStore: Store<SessionState, Unit>) {
    entry<Screen.DesignComponents> {
        DesignComponentsScreen(
            sessionStore = sessionStore,
        )
    }
    entry<Screen.DesignColors> {
        DesignColorsScreen(
            sessionStore = sessionStore,
            store = viewModel { DesignScreenStore() },
        )
    }
    entry<Screen.DesignShapes> {
        DesignShapesScreen(
            sessionStore = sessionStore,
            store = viewModel { DesignScreenStore() },
        )
    }
    entry<Screen.DesignTypography> {
        DesignTypographyScreen(
            sessionStore = sessionStore,
        )
    }
}
