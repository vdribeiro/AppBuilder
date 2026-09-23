package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.audio.AudioScreen

/**
 * The audio routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.audioProvider(useCases: UseCases) {
    entry<Screen.Audio> {
        val router = LocalRouter.current
        AudioScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(title = "audio"), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.AUDIO), router = router, authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
