package com.app.builder.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.container.Scaffold
import com.app.builder.ui.navigation.NavigationHandler
import com.app.builder.ui.navigation.navigationHandler

/**
 * A state holder that allows child screens to send their system bars up a master screen.
 *
 * @property onBackClick Lambda for back navigation.
 * @property topBar A composable slot to render an app bar widget at the top edge of the screen.
 * @property bottomBar A composable slot to render a navigation bar widget at the bottom edge of the screen.
 * @property snackbarHost A composable slot responsible for displaying transient overlay alerts.
 */
data class ScaffoldState(
    val onBackClick: (() -> Unit)? = null,
    val topBar: (@Composable ColumnScope.() -> Unit)? = null,
    val bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    val snackbarHost: @Composable () -> Unit = {},
)

/**
 * A CompositionLocal that provides a sink for screens to send their [ScaffoldState] upwards.
 * If this is null, the screen is responsible for drawing its own Scaffold.
 */
val LocalScaffold = compositionLocalOf<((ScaffoldState) -> Unit)?> { null }

/**
 * Primary entry point for screen implementations.
 *
 * @param modifier [Modifier] applied to the root container.
 * @param contentAlignment The alignment of the main content within the screen's body.
 * @param onBackClick Lambda for back navigation.
 * @param topBar Content slot for the top section.
 * @param bottomBar Content slot for the bottom section.
 * @param snackbarHost Container for displaying transient Snackbar notifications.
 * @param content The main screen content.
 * @see Skeleton
 */
@Composable
fun Screen(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    onBackClick: (() -> Unit)? = null,
    topBar: (@Composable ColumnScope.() -> Unit)? = null,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {}
) {
    when (val sink = LocalScaffold.current) {
        null -> Skeleton(
            modifier = modifier,
            contentAlignment = contentAlignment,
            onBackClick = onBackClick,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            content = content
        )

        else -> {
            // Parent is managing the Scaffold
            DisposableEffect(
                onBackClick,
                topBar,
                bottomBar,
                snackbarHost
            ) {
                sink(
                    ScaffoldState(
                        onBackClick = onBackClick,
                        topBar = topBar,
                        bottomBar = bottomBar,
                        snackbarHost = snackbarHost
                    )
                )
                onDispose {}
            }
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = contentAlignment
            ) {
                content()
            }
        }
    }
}

/**
 * Foundational UI [Scaffold] that integrates the Navigation service and provides a standardized structure for all screens, ensuring consistent handling of system bars.
 * It automatically handles [statusBarsPadding] for the [topBar] and [navigationBarsPadding] for the [bottomBar].
 * It provides default navigation behaviors, while also handling hardware-specific interactions like the physical back button and mouse side-buttons.
 *
 * @param modifier [Modifier] applied to the root container.
 * @param contentAlignment The alignment of the main content within the screen's body.
 * @param onBackClick Lambda for back navigation.
 * @param topBar Content slot for the top section.
 * @param bottomBar Content slot for the bottom section.
 * @param snackbarHost Container for displaying transient Snackbar notifications.
 * @param content The main screen content.
 */
@Composable
fun Skeleton(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    onBackClick: (() -> Unit)? = null,
    topBar: (@Composable ColumnScope.() -> Unit)? = null,
    bottomBar: (@Composable ColumnScope.() -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit = {}
) {
    if (onBackClick != null) NavigationHandler(onBack = onBackClick)
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .then(other = if (onBackClick != null) Modifier.navigationHandler(onBack = onBackClick) else Modifier),
        topBar = {
            topBar?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    it()
                }
            }
        },
        bottomBar = {
            bottomBar?.let {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    it()
                }
            }
        },
        snackbarHost = snackbarHost
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding)
                .padding(all = 16.dp),
            contentAlignment = contentAlignment
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun ScreenPreview() = Preview {
    Screen()
}
