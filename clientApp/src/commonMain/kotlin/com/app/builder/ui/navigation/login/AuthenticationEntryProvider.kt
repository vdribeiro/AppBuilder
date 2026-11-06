package com.app.builder.ui.navigation.login

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen

/**
 * The authentication routes.
 *
 * @param useCases The use cases.
 */
fun EntryProviderScope<NavKey>.authenticationProvider(useCases: UseCases) {
    entry<Screen.Login> {
        LoginScreen(store = viewModel { LoginScreenStore(state = LoginScreenState(), authenticationUseCases = useCases.authenticationUseCases) })
    }
}
