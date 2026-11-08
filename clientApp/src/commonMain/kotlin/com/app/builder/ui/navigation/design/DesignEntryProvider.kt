package com.app.builder.ui.navigation.design

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.hostSessionStore

/**
 * The design section routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.designProvider(useCases: UseCases) {
    entry<Screen.DesignComponents> {
        DesignComponentsScreen(
            sessionStore = hostSessionStore(useCases = useCases),
        )
    }
    entry<Screen.DesignColors> {
        DesignColorsScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            store = viewModel { DesignScreenStore() },
        )
    }
    entry<Screen.DesignShapes> {
        DesignShapesScreen(
            sessionStore = hostSessionStore(useCases = useCases),
            store = viewModel { DesignScreenStore() },
        )
    }
    entry<Screen.DesignTypography> {
        DesignTypographyScreen(
            sessionStore = hostSessionStore(useCases = useCases),
        )
    }
}
