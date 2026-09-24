package com.app.builder.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.data.resource.ImageResource
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Home Screen.
 *
 * @param actionBarStore Drives the top action bar.
 * @param navigationStore Drives the bottom navigation bar.
 */
@Composable
fun HomeScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
) {
    Screen(
        contentAlignment = Alignment.Center,
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        Image(image = ImageResource.Kotlin.toImage())
        Text(text = "Home")
    }
}

@Preview
@Composable
private fun HomeScreenPreview() = Preview {
    HomeScreen(
        actionBarStore = Store(initialState = ActionBarState()),
        navigationStore = Store(initialState = NavigationState()),
    )
}
