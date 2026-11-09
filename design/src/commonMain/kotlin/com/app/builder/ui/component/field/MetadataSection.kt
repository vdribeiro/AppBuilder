package com.app.builder.ui.component.field

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.divider.Divider

/**
 * The read-only data set apart below a divider.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param entries Pairs of translated label and value.
 */
@Composable
fun MetadataSection(
    modifier: Modifier = Modifier,
    entries: ImmutableList<Pair<String, String?>> = persistentListOf(),
) {
    val visible = entries.filter { it.second != null }
    if (visible.isEmpty()) return

    Column(modifier = modifier) {
        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        visible.forEach { (label, value) ->
            FieldValue(label = label, value = value, maxLines = 1)
        }
    }
}

@PreviewLightDark
@Composable
private fun MetadataSectionPreview() = Preview {
    MetadataSection(
        entries = persistentListOf(
            "UUID" to "6f1c2a8e-0b7d-4c2e-9a51-3f0e6d4b8c21",
            "Modified At" to "2026-11-09T10:00:00Z",
            "Deleted At" to null,
        )
    )
}
