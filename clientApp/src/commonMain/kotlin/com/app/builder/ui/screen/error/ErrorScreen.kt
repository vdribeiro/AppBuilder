package com.app.builder.ui.screen.error

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Console
import com.app.builder.ui.AppState
import com.app.builder.ui.LocalAppState
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.component.container.Feedback
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen
import com.app.builder.ui.navigation.authenticatedScreen
import com.app.builder.ui.navigation.loadingScreen
import com.app.builder.ui.navigation.unauthenticatedScreen
import com.app.builder.ui.screen.Screen

/**
 * The Error Screen.
 *
 * @param error true if the user was redirected to this screen because of an error, false if the user actively navigated to this screen.
 */
@Composable
fun ErrorScreen(error: Boolean) {
    val router = LocalRouter.current
    val appState = LocalAppState.current

    Screen(
        modifier = Modifier.imePadding(),
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = {
            TopActionBar(
                title = "feedback",
                onBackClick = {
                    if (error) {
                        val screen = when (appState) {
                            AppState.LOADING -> loadingScreen
                            AppState.UNAUTHENTICATED -> unauthenticatedScreen
                            AppState.AUTHENTICATED -> authenticatedScreen
                        }
                        router.navigate(screen = screen, option = Router.NavOption.CLEAR)
                    } else router.back()
                }
            )
        }
    ) {
        var logs: String? by remember { mutableStateOf(value = null) }
        if (ClientFlags.flags.console) LaunchedEffect(key1 = Unit) {
            while (isActive) {
                logs = Console.getSnapshot().joinToString(separator = "\n").ifBlank { null }
                delay(timeMillis = 1000)
            }
        }

        Feedback(
            logs = logs,
            error = error
        )
    }
}

@Preview
@Composable
private fun ErrorScreenPreview() = Preview {
    ErrorScreen(error = true)
}
