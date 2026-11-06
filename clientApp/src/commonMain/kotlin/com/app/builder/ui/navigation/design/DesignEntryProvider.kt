package com.app.builder.ui.navigation.design

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The design section routes.
 *
 * @param navigationStore The navigation bar store shared by every screen.
 */
fun EntryProviderScope<NavKey>.designProvider(navigationStore: Store<NavigationState, Unit>) {
    entry<Screen.DesignComponents> {
        DesignComponentsScreen(navigationStore = navigationStore)
    }
    entry<Screen.DesignColors> {
        DesignColorsScreen(navigationStore = navigationStore, store = viewModel { DesignScreenStore() })
    }
    entry<Screen.DesignShapes> {
        DesignShapesScreen(navigationStore = navigationStore, store = viewModel { DesignScreenStore() })
    }
    entry<Screen.DesignTypography> {
        DesignTypographyScreen(navigationStore = navigationStore)
    }
}
