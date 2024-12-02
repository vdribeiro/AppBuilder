package com.app.builder.ui.core.overlay

import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.ui.Preview
import com.app.builder.ui.core.container.Scaffold

/**
 * A wrapper around Material 3's [SnackbarHost] that exposes a state-free, declarative API.
 *
 * This component manages its own [SnackbarHostState] and coordinates the presentation duration through a [LaunchedEffect] and coroutine timeout based on the provided parameters.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param message The text content to display inside the snackbar.
 * @param buttonText The text label for an optional action button. If null, no action button is rendered.
 * @param durationMillis The amount of time in milliseconds before the snackbar is programmatically dismissed. Defaults to [Long.MAX_VALUE] if an action button is present, or 3000ms if it is text-only.
 * @param onDismiss A callback invoked when the snackbar times out or is dismissed from the screen.
 */
@Composable
fun Snackbar(
    modifier: Modifier = Modifier,
    message: String? = null,
    buttonText: String? = null,
    durationMillis: Long = if (buttonText != null) Long.MAX_VALUE else 3000L,
    onDismiss: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = message) {
        withTimeoutOrNull(timeMillis = durationMillis) {
            snackbarHostState.showSnackbar(
                message = message.orEmpty(),
                actionLabel = buttonText,
                duration = SnackbarDuration.Indefinite
            )
        }
        onDismiss()
    }
    SnackbarHost(
        modifier = modifier,
        hostState = snackbarHostState,
    )
}

@Preview
@Composable
private fun SnackbarPreview() = Preview {
    Scaffold(
        snackbarHost = {
            Snackbar(
                message = "Snackbar"
            )
        }
    )
}

@Preview
@Composable
private fun SnackbarWithButtonPreview() = Preview {
    Scaffold(
        snackbarHost = {
            Snackbar(
                message = "Snackbar",
                buttonText = "Action"
            )
        }
    )
}
