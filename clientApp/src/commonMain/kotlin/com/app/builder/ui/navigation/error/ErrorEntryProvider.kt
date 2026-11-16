package com.app.builder.ui.navigation.error

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.navigation.Screen

/** The error routes. */
fun EntryProviderScope<NavKey>.errorProvider() {
    entry<Screen.Error> {
        ErrorScreen(store = viewModel { ErrorScreenStore(state = ErrorScreenState(error = it.error)) })
    }
}
