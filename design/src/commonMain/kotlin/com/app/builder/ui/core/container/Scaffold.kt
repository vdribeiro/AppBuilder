package com.app.builder.ui.core.container

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.ui.Preview
import com.app.builder.ui.core.text.Text
import androidx.compose.material3.Scaffold as MaterialScaffold

/**
 * A structural layout container that provides slot coordinates for standard user interface structures such as top bars, bottom bars, and snackbar hosts,
 * while automatically calculating and passing safe inner content padding offsets via [PaddingValues] to prevent content overlaps.
 *
 * @param modifier The [Modifier] to be applied to the root scaffold layout surface.
 * @param topBar A composable slot to render an app bar widget at the top edge of the screen.
 * @param bottomBar A composable slot to render a navigation bar widget at the bottom edge of the screen.
 * @param snackbarHost A composable slot responsible for displaying transient overlay alerts.
 * @param content The primary page content composable lambda, receiving the calculated [PaddingValues] to ensure correct padding adjustments.
 */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit = {}
) {
    MaterialScaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        content = content
    )
}

@Preview
@Composable
private fun ScaffoldPreview() = Preview {
    Scaffold(
        topBar = { Text(text = "Top Bar") },
        bottomBar = { Text(text = "Bottom Bar") },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Content")
            }
        }
    )
}
