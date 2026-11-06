package com.app.builder.ui.navigation.userprofile

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen

/**
 * The user profile routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.userProfileProvider(useCases: UseCases) {
    entry<Screen.UserProfile> {
        UserProfileScreen(
            store = viewModel { UserProfileScreenStore(state = UserProfileScreenState(), authenticationUseCases = useCases.authenticationUseCases) },
        )
    }
}
