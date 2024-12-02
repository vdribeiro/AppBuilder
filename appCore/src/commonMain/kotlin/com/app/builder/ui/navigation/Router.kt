package com.app.builder.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Handles routing logic and manages the navigation backstack for the application. */
interface Router {

    /** The mutable backstack. Mutations can be applied directly to this property or through the [navigate] function. */
    val backStack: NavBackStack<NavKey>

    /** Pops the top screen off the stack. Does nothing if only one screen remains. */
    fun back()

    /**
     * Navigates to a new screen.
     *
     * @param screen The destination screen key.
     * @param option The strategy used to modify the stack. Defaults to [NavOption.ADD].
     */
    fun navigate(screen: NavKey, option: NavOption = NavOption.ADD)

    /** Defines the backstack mutation strategy when navigating to a screen. */
    enum class NavOption {
        /** Pushes the screen onto the top of the backstack. */
        ADD,

        /** Removes all screens from the backstack and pushes the new screen instance. */
        CLEAR,

        /**
         * Finds the first instance of the screen, removes it and all screens above it, then pushes the new screen instance.
         * This destroys the original screen and pushes a new one. Any transient UI state from the original screen will be lost.
         */
        REPLACE_FIRST,

        /**
         * Finds the last instance of the screen, removes it and all screens above it, then pushes the new screen instance.
         * This destroys the original screen and pushes a new one. Any transient UI state from the original screen will be lost.
         */
        REPLACE_LAST,

        /** Finds the last instance of the screen, and brings it to the top of the stack. */
        SWAP,

        /**
         * Finds the last instance of the screen and removes all screens above it.
         * The new screen instance passed to the navigate function is discarded as well as its parameters.
         */
        POP,

        /** Pushes the screen onto the top of the backstack only if it is not already on top. */
        IGNORE,
    }
}