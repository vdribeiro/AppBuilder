package com.app.builder.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** A no-op [Router] implementation. */
class NoOpRouter: Router {
    override val backStack: NavBackStack<NavKey> = NavBackStack()
    override fun back() {}
    override fun navigate(screen: NavKey, option: Router.NavOption) {}
}