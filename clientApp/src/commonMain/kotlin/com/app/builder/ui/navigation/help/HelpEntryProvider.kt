package com.app.builder.ui.navigation.help

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.ui.navigation.Screen

/** The help routes. */
fun EntryProviderScope<NavKey>.helpProvider() {
    entry<Screen.Help> { HelpScreen() }
}
