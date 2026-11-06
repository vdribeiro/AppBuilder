package com.app.builder.ui.navigation.camera

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The camera routes.
 *
 * @param useCases The use cases.
 * @param navigationStore The navigation bar store shared by every screen.
 */
fun EntryProviderScope<NavKey>.cameraProvider(useCases: UseCases, navigationStore: Store<NavigationState, Unit>) {
    entry<Screen.Camera> {
        val router = LocalRouter.current
        CameraScreen(
            actionBarStore = viewModel { ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = useCases.authenticationUseCases, statusUseCases = useCases.statusUseCases) },
            navigationStore = navigationStore,
        )
    }
}
