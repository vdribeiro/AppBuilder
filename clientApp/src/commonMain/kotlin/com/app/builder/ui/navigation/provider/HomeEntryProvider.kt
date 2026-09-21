package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.home.HomeScreen

/**
 * The home routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.homeProvider(useCases: UseCases) {
    entry<Screen.Home> {
        val router = LocalRouter.current
        HomeScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "home"), router = router, authenticationUseCases = useCases.authenticationUseCases) }
        )
    }
}
