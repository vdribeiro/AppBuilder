package com.app.builder.ui.navigation.userprofile

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.store.Store

/**
 * The user profile routes.
 *
 * @param useCases The use cases.
 * @param navigationStore The navigation bar store shared by every screen.
 */
fun EntryProviderScope<NavKey>.userProfileProvider(useCases: UseCases, navigationStore: Store<NavigationState, Unit>) {
    entry<Screen.UserProfile> {
        UserProfileScreen(
            navigationStore = navigationStore,
            store = viewModel { UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
