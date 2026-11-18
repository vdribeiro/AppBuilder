package com.app.builder.ui.navigation.error

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.app.builder.ui.store.Store
import com.app.builder.ui.store.previewStore

/**
 * The Error Screen.
 *
 * @param store Provides the error screen state.
 */
@Composable
fun ErrorScreen(store: Store<ErrorScreenState, Unit>) {
    val state by store.stateFlow.collectAsStateWithLifecycle()
    val router = LocalRouter.current
    val appState = LocalAppState.current

    Screen(
        onBackClick = { router.navigate(screen = Screen.Home, option = Router.NavOption.CLEAR) },
        topBar = {
            TopActionBar(
                title = "feedback",
                onBackClick = {
                    if (state.error) {
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
        Feedback(
            logs = state.logs,
            error = state.error
        )
    }
}

@PreviewLightDark
@Composable
private fun ErrorScreenPreview() = Preview {
    ErrorScreen(
        store = previewStore(initialState = ErrorScreenState(error = true, logs = "Some very interesting logs")),
    )
}
