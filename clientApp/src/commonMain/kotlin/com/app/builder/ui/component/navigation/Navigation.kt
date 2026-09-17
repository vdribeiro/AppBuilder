package com.app.builder.ui.component.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.ui.Preview
import com.app.builder.ui.core.navigation.NavigationBar
import com.app.builder.ui.store.Store

/**
 * Renders the app's navigation bar from the given [store]'s [NavigationState].
 *
 * @param store The store providing the [NavigationState] items to render.
 */
@Composable
fun Navigation(store: Store<NavigationState, Unit>) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    NavigationBar(
        modifier = Modifier
            .testTag(tag = "navigation_bar")
            .fillMaxWidth(),
        items = state.items
    )
}

@Preview
@Composable
private fun NavigationPreview() = Preview {
    Navigation(
        store = Store(
            initialState = NavigationState()
        )
    )
}
