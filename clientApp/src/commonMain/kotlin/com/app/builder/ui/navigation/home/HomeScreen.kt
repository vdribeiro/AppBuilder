package com.app.builder.ui.navigation.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.data.resource.ImageResource
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The Home Screen.
 *
 * @param sessionStore The session store shared by every screen.
 */
@Composable
fun HomeScreen(
    sessionStore: Store<SessionState, Unit>,
) {
    Screen(
        contentAlignment = Alignment.Center,
        topBar = { ActionBar(sessionStore = sessionStore) },
        bottomBar = { Navigation(sessionStore = sessionStore, selected = NavigationRoute.HOME) },
    ) {
        Image(image = ImageResource.Kotlin.toImage())
        Text(text = "Home")
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() = Preview {
    HomeScreen(
        sessionStore = previewStore(initialState = SessionState()),
    )
}
