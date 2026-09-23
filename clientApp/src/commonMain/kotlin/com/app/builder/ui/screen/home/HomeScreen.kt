package com.app.builder.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.data.resource.ImageResource
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.screen.Screen

/** The Home Screen. */
@Composable
fun HomeScreen() {
    val router = LocalRouter.current
    Screen(
        contentAlignment = Alignment.Center,
        topBar = {
            TopActionBar(
                avatarImage = ImageResource.Kotlin.toImage(),
                onAvatarClick = { router.navigate(screen = Screen.UserProfile) },
                onLoginClick = { router.navigate(screen = Screen.Login) }
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
    HomeScreen()
}
