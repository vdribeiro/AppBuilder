package com.app.builder.ui.component.card

import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.field.LinkSection
import com.app.builder.ui.component.field.MetadataSection
import com.app.builder.ui.component.field.TextField
import com.app.builder.ui.core.card.Card

/**
 * A material card, showing only the properties that are not `null`.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Whether the material's properties can be edited.
 * @param uuid The UUID of the material.
 * @param modifiedAt The modification timestamp of the material, as a UTC ISO-8601 string shown as a localized date-time.
 * @param deletedAt The deletion timestamp of the material, as a UTC ISO-8601 string shown as a localized date-time.
 * @param name The name of the material.
 * @param onNameChange Callback for when the name changes.
 * @param description The description of the material.
 * @param onDescriptionChange Callback for when the description changes.
 * @param links A slot rendered above the metadata, typically a [LinkSection].
 */
@Composable
fun MaterialCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    uuid: String? = null,
    modifiedAt: String? = null,
    deletedAt: String? = null,
    name: String? = null,
    onNameChange: (String) -> Unit = {},
    description: String? = null,
    onDescriptionChange: (String) -> Unit = {},
    links: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            name?.let {
                TextField(
                    label = "material_name",
                    value = it,
                    editing = enabled,
                    onValueChange = onNameChange
                )
            }
            description?.let {
                TextField(
                    label = "material_description",
                    value = it,
                    editing = enabled,
                    onValueChange = onDescriptionChange,
                    maxLines = Int.MAX_VALUE
                )
            }
            links()
            MetadataSection(
                entries = persistentListOf(
                    "material_uuid" to uuid,
                ),
                dateTimeEntries = persistentListOf(
                    "material_modified_at" to modifiedAt,
                    "material_deleted_at" to deletedAt,
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MaterialCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "material_name" to "Name",
            "material_description" to "Description",
            "material_modified_at" to "Modified At",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        MaterialCard(
            enabled = false,
            modifiedAt = "2026-11-09T10:00:00Z",
            name = "Copper Cable",
            description = "2.5 mm² single core"
        )
        MaterialCard(
            enabled = true,
            name = "Copper Cable",
            description = "2.5 mm² single core"
        )
    }
}

@PreviewLightDark
@Composable
private fun MaterialCardNullablePreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "material_name" to "Name",
            "material_description" to "Description",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        MaterialCard(
            name = "Material",
        )
        MaterialCard(
            description = "Noice!"
        )
    }
}
