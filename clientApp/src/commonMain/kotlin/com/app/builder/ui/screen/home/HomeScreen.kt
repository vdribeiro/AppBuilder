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
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Home Screen.
 *
 * @param actionBarStore The store for action bar.
 */
@Composable
fun HomeScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
) {
    Screen(
        contentAlignment = Alignment.Center,
        topBar = { ActionBar(store = actionBarStore) },
    ) {
        Image(image = ImageResource.Kotlin.toImage())
        Text(text = "Home")
    }
}

@Preview
@Composable
private fun HomeScreenPreview() = Preview {
    HomeScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Robotnik")),
    )
}
