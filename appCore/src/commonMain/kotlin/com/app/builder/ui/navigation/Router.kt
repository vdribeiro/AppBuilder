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
         * Finds the first instance of the screen type, removes it and all screens above it, then pushes the new screen instance.
         * The removed screens lose their transient UI state, unless an equal screen is still in the backstack afterwards,
         * such as when the pushed screen equals the one it replaces. Equal screens share the same state.
         */
        REPLACE_FIRST,

        /**
         * Finds the last instance of the screen type, removes it and all screens above it, then pushes the new screen instance.
         * The removed screens lose their transient UI state, unless an equal screen is still in the backstack afterwards,
         * such as when the pushed screen equals the one it replaces. Equal screens share the same state.
         */
        REPLACE_LAST,

        /**
         * Finds the last instance of the screen type, and brings it to the top of the stack, discarding the new screen instance.
         * Pushes the new screen instance when no screen of the same type is in the backstack.
         */
        SWAP,

        /**
         * Finds the last instance of the screen type and removes all screens above it.
         * Does nothing when no screen of the same type is in the backstack.
         * The new screen instance passed to the navigate function is discarded as well as its parameters.
         */
        POP,

        /** Pushes the screen onto the top of the backstack only if the top screen is not of the same type. */
        IGNORE,
    }
}