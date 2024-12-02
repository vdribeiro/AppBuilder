package com.app.builder.ui.navigation.provider

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.domain.gateway.UseCases
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.login.LoginScreen
import com.app.builder.ui.screen.login.LoginScreenState
import com.app.builder.ui.screen.login.LoginScreenStore

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
