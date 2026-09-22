package com.app.builder.ui.component.container

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getTranslation

/**
 * A feedback form with a [Console].
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param logs The logs to show in the console.
 * @param error true to show an error message.
 */
@Composable
fun Feedback(
    modifier: Modifier = Modifier,
    logs: String? = null,
    error: Boolean = false
) {
    val typography = LocalTypography.current

    var showThanks: Boolean by remember { mutableStateOf(value = false) }
    var feedback: String by remember { mutableStateOf(value = "") }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Icon(
            modifier = Modifier
                .size(size = 64.dp),
            imageVector = Icons.Outlined.BugReport,
            contentDescription = "Feedback Icon",
        )

        val text = if (error) getTranslation(key = "system_failure", default = "An unexpected error occurred.\nPlease contact your system administrator.") else {
            getTranslation(key = "feedback", default = "Your feedback is important and extremely appreciated!\nThank you!")
        }
        Text(
            text = text,
            translate = false,
            style = typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Input(
            modifier = Modifier
                .testTag(tag = "feedback_input")
                .fillMaxWidth()
                .height(height = 120.dp),
            enabled = true,
            value = feedback,
            onValueChange = {
                feedback = it
                showThanks = false
            },
        )
        val thanks = getTranslation(key = "thanks").takeIf { it != "thanks" } ?: "Thank you for your feedback"
        val send = getTranslation(key = "send").takeIf { it != "send" } ?: "Send"
        Button(
            enabled = !showThanks && feedback.isNotBlank(),
            text = if (showThanks) thanks else send,
            onClick = {
                Telemetry.feedback(message = feedback)
                showThanks = true
            },
        )
        logs?.let {
            Console(
                modifier = Modifier
                    .weight(weight = 1f)
                    .padding(
                        top = 16.dp,
                        bottom = 16.dp
                    ),
                logs = it
            )
        }
    }
}

@Preview
@Composable
private fun FeedbackPreview() = Preview {
    Feedback(error = false)
}

@Preview
@Composable
private fun FeedbackErrorPreview() = Preview {
    Feedback(
        logs = "Some very interesting logs",
        error = true
    )
}
