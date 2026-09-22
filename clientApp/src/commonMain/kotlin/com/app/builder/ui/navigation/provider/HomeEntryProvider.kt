package com.app.builder.ui.navigation.provider

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.home.HomeScreen

/** The home routes. */
fun EntryProviderScope<NavKey>.homeProvider() {
    entry<Screen.Home> {
        HomeScreen()
    }
}
