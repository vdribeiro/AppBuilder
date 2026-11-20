package com.app.builder.ui.navigation.help

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.container.Help
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.screen.Screen

/** The Help Screen, explaining the status icons. */
@Composable
fun HelpScreen() {
    val router = LocalRouter.current

    Screen(
        onBackClick = { router.back() },
        topBar = {
            TopActionBar(
                title = "help_title",
                onBackClick = { router.back() }
            )
        },
    ) {
        Help()
    }
}

@PreviewLightDark
@Composable
private fun HelpScreenPreview() = Preview {
    HelpScreen()
}
