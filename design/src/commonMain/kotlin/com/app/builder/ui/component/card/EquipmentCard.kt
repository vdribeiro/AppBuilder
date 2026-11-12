package com.app.builder.ui.component.card

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.field.ChoiceField
import com.app.builder.ui.component.field.LinkSection
import com.app.builder.ui.component.field.MetadataSection
import com.app.builder.ui.component.field.TextField
import com.app.builder.ui.core.card.Card

/**
 * An equipment card, showing only the properties that are not `null`.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Whether the equipment's properties can be edited.
 * @param uuid The UUID of the equipment.
 * @param modifiedAt The modification timestamp of the equipment.
 * @param deletedAt The deletion timestamp of the equipment.
 * @param name The name of the equipment.
 * @param onNameChange Callback for when the name changes.
 * @param description The description of the equipment.
 * @param onDescriptionChange Callback for when the description changes.
 * @param status The equipment's status, shown as selected among [statuses].
 * @param statuses Every status the equipment can be in.
 * @param onStatusChange Callback for when a status is selected.
 * @param links A slot rendered above the metadata, typically a [LinkSection].
 */
@Composable
fun EquipmentCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    uuid: String? = null,
    modifiedAt: String? = null,
    deletedAt: String? = null,
    name: String? = null,
    onNameChange: (String) -> Unit = {},
    description: String? = null,
    onDescriptionChange: (String) -> Unit = {},
    status: String? = null,
    statuses: ImmutableList<String> = persistentListOf(),
    onStatusChange: (String) -> Unit = {},
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
                    label = "equipment_name",
                    value = it,
                    editing = enabled,
                    onValueChange = onNameChange
                )
            }
            description?.let {
                TextField(
                    label = "equipment_description",
                    value = it,
                    editing = enabled,
                    onValueChange = onDescriptionChange,
                    maxLines = Int.MAX_VALUE
                )
            }
            status?.let {
                ChoiceField(
                    modifier = Modifier.testTag(tag = "equipment_status"),
                    label = "equipment_status",
                    selected = it,
                    options = statuses,
                    editing = enabled,
                    onSelect = onStatusChange
                )
            }
            links()
            MetadataSection(
                entries = persistentListOf(
                    "equipment_uuid" to uuid,
                    "equipment_modified_at" to modifiedAt,
                    "equipment_deleted_at" to deletedAt,
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun EquipmentCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "equipment_name" to "Name",
            "equipment_description" to "Description",
            "equipment_status" to "Status",
            "equipment_status_working" to "Working",
            "equipment_status_malfunctioning" to "Malfunctioning",
            "equipment_modified_at" to "Modified At",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        EquipmentCard(
            enabled = false,
            modifiedAt = "2026-11-09T10:00:00Z",
            name = "Equipment",
            description = "Noice!",
            status = "equipment_status_working",
            statuses = persistentListOf("equipment_status_working", "equipment_status_malfunctioning")
        )
        EquipmentCard(
            enabled = true,
            name = "Equipment",
            description = "Noice!",
            status = "equipment_status_malfunctioning",
            statuses = persistentListOf("equipment_status_working", "equipment_status_malfunctioning")
        )
    }
}

@PreviewLightDark
@Composable
private fun EquipmentCardNullablePreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "equipment_name" to "Name",
            "equipment_description" to "Description",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        EquipmentCard(
            name = "Equipment",
        )
        EquipmentCard(
            description = "Noice!"
        )
    }
}
