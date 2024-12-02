package com.app.builder.ui.navigation.provider

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.error.ErrorScreen

/** The error routes. */
fun EntryProviderScope<NavKey>.errorProvider() {
    entry<Screen.Error> { ErrorScreen(error = it.error) }
}
