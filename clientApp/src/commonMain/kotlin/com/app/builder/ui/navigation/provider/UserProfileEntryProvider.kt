package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.userprofile.UserProfileScreen
import com.app.builder.ui.screen.userprofile.UserProfileScreenState
import com.app.builder.ui.screen.userprofile.UserProfileScreenStore

/**
 * The user profile routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.userProfileProvider(useCases: UseCases) {
    entry<Screen.UserProfile> {
        val router = LocalRouter.current
        UserProfileScreen(
            navigationStore = viewModel { NavigationStore(state = NavigationState(), router = router, authenticationUseCases = useCases.authenticationUseCases) },
            store = viewModel { UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
