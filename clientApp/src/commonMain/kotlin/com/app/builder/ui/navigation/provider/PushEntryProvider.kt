package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.push.PushScreen
import com.app.builder.ui.screen.push.PushScreenState
import com.app.builder.ui.screen.push.PushScreenStore
import com.app.builder.ui.store.Store

/**
 * The push notifications route.
 *
 * @param useCases The use cases.
 * @param navigationStore The navigation bar store shared by every screen.
 */
fun EntryProviderScope<NavKey>.pushProvider(useCases: UseCases, navigationStore: Store<NavigationState, Unit>) {
    entry<Screen.Push> {
        val router = LocalRouter.current
        PushScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = useCases.authenticationUseCases, statusUseCases = useCases.statusUseCases) },
            navigationStore = navigationStore,
            store = viewModel { PushScreenStore(state = PushScreenState(), pushUseCases = useCases.pushUseCases, userUseCases = useCases.userUseCases) },
        )
    }
}
