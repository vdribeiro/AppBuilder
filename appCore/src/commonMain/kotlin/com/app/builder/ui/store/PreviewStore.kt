package com.app.builder.ui.store

/**
 * Creates a [Store] for composable previews.
 *
 * @param initialState The state the preview renders.
 */
fun <State, Action> previewStore(initialState: State): Store<State, Action> =
    Store(initialState = initialState)
