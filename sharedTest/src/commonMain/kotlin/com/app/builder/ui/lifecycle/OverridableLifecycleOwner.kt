package com.app.builder.ui.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/** A [LifecycleOwner] that can be manually set to a specific state. */
class OverridableLifecycleOwner(initialState: Lifecycle.State = Lifecycle.State.RESUMED): LifecycleOwner {
    override val lifecycle = LifecycleRegistry(provider = this)

    init {
        setCurrentState(state = initialState)
    }

    /** Moves the lifecycle to the given [state]. */
    fun setCurrentState(state: Lifecycle.State) {
        lifecycle.currentState = state
    }
}
