package com.app.builder.ui.component.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.text.Text

/**
 * Displays scrollable stacktrace logs, automatically scrolling to the bottom as new [logs] arrive.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param logs The logs to display.
 */
@Composable
fun Console(
    modifier: Modifier = Modifier,
    logs: String = ""
) {
    val typography = LocalTypography.current

    val scrollState = rememberScrollState()
    LaunchedEffect(key1 = logs) { scrollState.scrollTo(value = scrollState.maxValue) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = "stacktrace",
            style = typography.labelLarge
        )
        SelectionContainer {
            Text(
                modifier = Modifier
                    .testTag("console_logs")
                    .fillMaxWidth()
                    .verticalScroll(state = scrollState),
                text = logs,
                style = typography.labelSmall
            )
        }
    }
}

@Preview
@Composable
private fun ConsolePreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "stacktrace" to "Stacktrace"
        )
    )
    Console(logs = "Some very interesting logs")
}
