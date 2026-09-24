package com.app.builder.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.data.resource.ImageResource
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Home Screen.
 *
 * @param navigationStore Drives the bottom navigation bar.
 * @param store Provides the home state.
 */
@Composable
fun HomeScreen(
    navigationStore: Store<NavigationState, Unit>,
    store: Store<HomeScreenState, Unit>,
) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val router = LocalRouter.current
    Screen(
        contentAlignment = Alignment.Center,
        bottomBar = if (state.guest) null else {
            { Navigation(store = navigationStore) }
        },
        topBar = {
            TopActionBar(
                avatarImage = state.image,
                onAvatarClick = { router.navigate(screen = Screen.UserProfile) },
                onLoginClick = if (state.guest) {
                    { router.navigate(screen = Screen.Login) }
                } else null
            )
        },
    ) {
        Image(image = ImageResource.Kotlin.toImage())
        Text(text = "Home")
    }
}

@Preview
@Composable
private fun HomeScreenPreview() = Preview {
    HomeScreen(
        navigationStore = Store(initialState = NavigationState()),
        store = Store(initialState = HomeScreenState()),
    )
}
