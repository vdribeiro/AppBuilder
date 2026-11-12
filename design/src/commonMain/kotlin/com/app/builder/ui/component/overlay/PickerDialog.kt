package com.app.builder.ui.component.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.bar.EntityActions
import com.app.builder.ui.component.bar.TopActionBar
import com.app.builder.ui.core.container.Scaffold
import com.app.builder.ui.core.overlay.FullScreenDialog
import com.app.builder.ui.core.text.Text

/**
 * A dialog taking up the whole screen to pick items from a list, with a top action bar showing a close button, the title, a save button and room for list filters.
 *
 * @param modifier The [Modifier] to be applied to the dialog's content.
 * @param tag The prefix of the test tags of this dialog and its buttons.
 * @param title The title of the dialog, translated.
 * @param onSave Callback for when the save button is clicked.
 * @param onDismiss Callback for when the dialog is closed without saving.
 * @param actions Content shown after the save button, such as a [com.app.builder.ui.component.bar.FiltersButton].
 * @param secondaryActions Content shown in a row below the bar, such as [com.app.builder.ui.component.bar.Filters], or `null` to hide the row.
 * @param content The list to pick from.
 */
@Composable
fun PickerDialog(
    modifier: Modifier = Modifier,
    tag: String = "picker",
    title: String = "",
    onSave: () -> Unit = {},
    onDismiss: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    secondaryActions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    FullScreenDialog(
        modifier = modifier.testTag(tag = tag),
        onDismissRequest = onDismiss,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopActionBar(
                    title = title,
                    onBackClick = onDismiss,
                    actions = {
                        EntityActions(onSaveClick = onSave)
                        actions()
                    },
                    secondaryActions = secondaryActions,
                )
            },
        ) { padding ->
            Box(modifier = Modifier.padding(paddingValues = padding)) { content() }
        }
    }
}

@PreviewLightDark
@Composable
private fun PickerDialogPreview() = Preview {
    InjectTranslations(translations = mapOf("building_link_equipments" to "Link equipments"))
    PickerDialog(
        title = "building_link_equipments",
    ) {
        Text(text = "List of equipments", translate = false)
    }
}
