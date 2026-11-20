package com.app.builder.ui.component.overlay

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.button.RadioButton
import com.app.builder.ui.core.overlay.Dialog
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getTranslation

/**
 * A dialog to pick the language the app is shown in, listing a system entry followed by the languages as radio options, with apply and cancel buttons.
 *
 * @param modifier The [Modifier] to be applied to the dialog.
 * @param languages The ISO codes of the languages the app can be shown in, shown translated.
 * @param selectedLanguage The ISO code of the ticked language, or `null` to tick the system entry.
 * @param onLanguageSelected Callback invoked with the ISO code of the language clicked, or `null` when the system entry is clicked.
 * @param onApply Callback for when the apply button is clicked.
 * @param onDismiss Callback for when the dialog is closed without applying.
 */
@Composable
fun LanguageDialog(
    modifier: Modifier = Modifier,
    languages: ImmutableList<String> = persistentListOf(),
    selectedLanguage: String? = null,
    onLanguageSelected: (String?) -> Unit = {},
    onApply: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    val typography = LocalTypography.current

    Dialog(
        modifier = modifier.testTag(tag = "language_dialog"),
        title = getTranslation(key = "language"),
        confirmText = getTranslation(key = "apply"),
        dismissText = getTranslation(key = "cancel"),
        onConfirm = onApply,
        onDismiss = onDismiss,
    ) {
        Column(modifier = Modifier.verticalScroll(state = rememberScrollState())) {
            (listOf(element = null) + languages).forEach { candidate ->
                Row(
                    modifier = Modifier
                        .testTag(tag = "language_${candidate ?: "system"}")
                        .fillMaxWidth()
                        .selectable(
                            selected = candidate == selectedLanguage,
                            role = Role.RadioButton,
                            onClick = { onLanguageSelected(candidate) }
                        )
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = candidate == selectedLanguage)
                    Text(text = getTranslation(key = candidate ?: "system"), maxLines = 1, style = typography.bodyLarge)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LanguageDialogPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "language" to "Language",
            "system" to "System",
            "en" to "English",
            "pt" to "Português",
            "apply" to "Apply",
            "cancel" to "Cancel"
        )
    )
    LanguageDialog(
        languages = persistentListOf("en", "pt"),
        selectedLanguage = "en"
    )
}
