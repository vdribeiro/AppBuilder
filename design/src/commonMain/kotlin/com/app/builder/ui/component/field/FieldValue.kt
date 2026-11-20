package com.app.builder.ui.component.field

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.getTranslation

/**
 * The read-only presentation shared by every field: a small muted label above its value.
 * A blank value shows as a dash, so the field keeps its place in the layout.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param label The label of the field, translated.
 * @param value The value of the field, shown as given.
 * @param maxLines The maximum number of lines of [value] before it is truncated.
 * @param selectable Whether [value] can be selected and copied.
 */
@Composable
fun FieldValue(
    modifier: Modifier = Modifier,
    label: String = "",
    value: String? = null,
    maxLines: Int = Int.MAX_VALUE,
    selectable: Boolean = false,
) {
    val typography = LocalTypography.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        Text(
            text = getTranslation(key = label),
            maxLines = 1,
            style = typography.labelMedium
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: EMPTY_VALUE,
            maxLines = maxLines,
            style = typography.bodyLarge,
            selectable = selectable
        )
    }
}

/** Shown in place of a blank value. */
private const val EMPTY_VALUE = "—"

@PreviewLightDark
@Composable
private fun FieldValuePreview() = Preview {
    Column {
        FieldValue(label = "Title", value = "Be an awesome person")
        FieldValue(label = "Description", value = "")
    }
}
