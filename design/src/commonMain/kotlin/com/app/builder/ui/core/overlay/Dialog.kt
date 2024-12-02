package com.app.builder.ui.core.overlay

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.text.Text

/**
 * A highly configuration-driven modal alert that provides localized confirmations, optional dismiss behaviors, and safe null-checks for text components.
 * This ensures dialog slots adapt automatically when textual properties are omitted.
 *
 * @param modifier The [Modifier] to be applied to the dialog's structural container layout.
 * @param title An optional heading title text string rendered at the top of the dialog.
 * @param text An optional primary body text description string detailing the message of the dialog.
 * @param confirmText The localized action button string for confirmations. Defaults to the translation for "yes".
 * @param dismissText An optional localized action button string for cancellations. Defaults to the translation for "no". If null, the secondary action button slot is hidden.
 * @param onConfirm Callback invoked when the user selects the primary confirmation button.
 * @param onDismiss Callback invoked when the user selects the secondary dismissal button.
 * @param onDismissRequest Callback invoked when the user attempts to close the dialog by clicking outside its bounds or pressing back. Defaults to forwarding to [onDismiss].
 */
@Composable
fun Dialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String? = null,
    confirmText: String = "yes",
    dismissText: String? = "no",
    onConfirm: () -> Unit = {},
    onDismiss: (() -> Unit) = {},
    onDismissRequest: () -> Unit = onDismiss,
) {
    val typography = LocalTypography.current

    AlertDialog(
        modifier = modifier,
        title = {
            title?.let {
                Text(
                    text = it,
                    textAlign = TextAlign.Start,
                    style = typography.titleLarge
                )
            }
        },
        text = {
            text?.let {
                Text(
                    text = it,
                    textAlign = TextAlign.Start,
                    style = typography.bodyLarge
                )
            }
        },
        confirmButton = { Button(text = confirmText, onClick = { onConfirm() }) },
        dismissButton = dismissText?.let { { Button(text = it, onClick = { onDismiss() }) } },
        onDismissRequest = onDismissRequest
    )
}

@Preview
@Composable
private fun DialogPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "yes" to "Yes",
            "no" to "No"
        )
    )
    Dialog(
        title = "Title",
        text = "Text"
    )
}
