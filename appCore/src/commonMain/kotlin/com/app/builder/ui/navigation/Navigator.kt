package com.app.builder.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.app.builder.core.telemetry.Telemetry

/** A [Router] implementation that uses a [NavBackStack] to navigate between screens. */
class Navigator(override val backStack: NavBackStack<NavKey>): Router {

    override fun back() {
        Telemetry.info(tag = TAG, message = "Navigating back")
        if (backStack.size > 1) backStack.removeLastOrNull()
        Telemetry.info(tag = TAG, message = "Navigation stack: ${backStack.joinToString(separator = " -> ")}")
    }

    override fun navigate(screen: NavKey, option: Router.NavOption) {
        Telemetry.info(tag = TAG, message = "Navigating to: $screen with option $option")
        when (option) {
            Router.NavOption.ADD -> backStack.add(element = screen)

            Router.NavOption.CLEAR -> {
                backStack.clear()
                backStack.add(element = screen)
            }

            Router.NavOption.REPLACE_FIRST -> {
                val index = backStack.indexOfFirst { it::class == screen::class }
                if (index != -1) backStack.subList(fromIndex = index, toIndex = backStack.size).clear()
                backStack.add(element = screen)
            }

            Router.NavOption.REPLACE_LAST -> {
                val index = backStack.indexOfLast { it::class == screen::class }
                if (index != -1) backStack.subList(fromIndex = index, toIndex = backStack.size).clear()
                backStack.add(element = screen)
            }

            Router.NavOption.SWAP -> {
                val index = backStack.indexOfLast { it::class == screen::class }
                val screen = if (index != -1) backStack.removeAt(index = index) else screen
                backStack.add(element = screen)
            }

            Router.NavOption.POP -> {
                val index = backStack.indexOfLast { it::class == screen::class }
                if (index != -1) backStack.subList(fromIndex = index + 1, toIndex = backStack.size).clear()
            }

            Router.NavOption.IGNORE -> {
                val last = backStack.lastOrNull()
                if (last == null || last::class != screen::class) backStack.add(element = screen)
            }
        }
        Telemetry.info(tag = TAG, message = "Navigation stack: ${backStack.joinToString(separator = " -> ")}")
    }

    companion object {
        private const val TAG = "Navigator"
    }
}