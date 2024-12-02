package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.component.useravatar.UserAvatarStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.register.RegisterScreen
import com.app.builder.ui.screen.register.RegistrationScreenState
import com.app.builder.ui.screen.register.RegistrationScreenStore

/**
 * The registration routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.userProvider(useCases: UseCases) {
    entry<Screen.UserList> {
        val router = LocalRouter.current
        // TODO
        RegisterScreen(
            navigationStore = viewModel { NavigationStore(state = NavigationState(selected = NavigationRoute.USER), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            userAvatarStore = viewModel { UserAvatarStore(state = UserAvatarState(), authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { RegistrationScreenStore(state = RegistrationScreenState(), authenticationUseCases = useCases.authenticationUseCases) }
        )
    }
}
