package com.app.builder.ui.component.card

import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.field.LinkSection
import com.app.builder.ui.component.field.MetadataSection
import com.app.builder.ui.component.field.TextField
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.image.Icon

/**
 * A building card, showing only the properties that are not `null`.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Whether the building's properties can be edited.
 * @param uuid The UUID of the building.
 * @param modifiedAt The modification timestamp of the building.
 * @param deletedAt The deletion timestamp of the building.
 * @param name The name of the building.
 * @param onNameChange Callback for when the name changes.
 * @param description The description of the building.
 * @param onDescriptionChange Callback for when the description changes.
 * @param address The postal address of the building.
 * @param onAddressChange Callback for when the address changes.
 * @param latitude The latitude of the building, as typed.
 * @param onLatitudeChange Callback for when the latitude changes.
 * @param latitudeError Whether the latitude is invalid.
 * @param longitude The longitude of the building, as typed.
 * @param onLongitudeChange Callback for when the longitude changes.
 * @param longitudeError Whether the longitude is invalid.
 * @param onUseLocationClick Callback for filling the coordinates, or `null` to hide it.
 * @param links A slot rendered above the metadata, typically a [LinkSection].
 */
@Composable
fun BuildingCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    uuid: String? = null,
    modifiedAt: String? = null,
    deletedAt: String? = null,
    name: String? = null,
    onNameChange: (String) -> Unit = {},
    description: String? = null,
    onDescriptionChange: (String) -> Unit = {},
    address: String? = null,
    onAddressChange: (String) -> Unit = {},
    latitude: String? = null,
    onLatitudeChange: (String) -> Unit = {},
    latitudeError: Boolean = false,
    longitude: String? = null,
    onLongitudeChange: (String) -> Unit = {},
    longitudeError: Boolean = false,
    onUseLocationClick: (() -> Unit)? = null,
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
                    label = "building_name",
                    value = it,
                    editing = enabled,
                    onValueChange = onNameChange
                )
            }
            description?.let {
                TextField(
                    label = "building_description",
                    value = it,
                    editing = enabled,
                    onValueChange = onDescriptionChange,
                    maxLines = Int.MAX_VALUE
                )
            }
            address?.let {
                TextField(
                    label = "building_address",
                    value = it,
                    editing = enabled,
                    onValueChange = onAddressChange,
                    maxLines = Int.MAX_VALUE
                )
            }
            latitude?.let {
                TextField(
                    modifier = Modifier.testTag(tag = "building_latitude"),
                    label = "building_latitude",
                    value = it,
                    editing = enabled,
                    onValueChange = onLatitudeChange,
                    keyboardType = KeyboardType.Decimal,
                    isError = latitudeError
                )
            }
            longitude?.let {
                TextField(
                    modifier = Modifier.testTag(tag = "building_longitude"),
                    label = "building_longitude",
                    value = it,
                    editing = enabled,
                    onValueChange = onLongitudeChange,
                    keyboardType = KeyboardType.Decimal,
                    isError = longitudeError
                )
            }
            if (enabled && onUseLocationClick != null) Button(
                modifier = Modifier
                    .testTag(tag = "building_use_location")
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                text = "building_use_location",
                content = { Icon(imageVector = Icons.Filled.MyLocation) },
                onClick = { onUseLocationClick() }
            )
            links()
            MetadataSection(
                entries = persistentListOf(
                    "building_uuid" to uuid,
                    "building_modified_at" to modifiedAt,
                    "building_deleted_at" to deletedAt,
                )
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BuildingCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_name" to "Name",
            "building_description" to "Description",
            "building_address" to "Address",
            "building_latitude" to "Latitude",
            "building_longitude" to "Longitude",
            "building_use_location" to "Use my location",
            "building_modified_at" to "Modified At",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        BuildingCard(
            enabled = false,
            modifiedAt = "2026-11-09T10:00:00Z",
            name = "Building",
            description = "Noice!",
            address = "Avenida dos Aliados, Porto",
            latitude = "41.1579",
            longitude = "-8.6291"
        )
        BuildingCard(
            enabled = true,
            name = "Building",
            description = "Noice!",
            address = "Avenida dos Aliados, Porto",
            latitude = "91",
            latitudeError = true,
            longitude = "",
            onUseLocationClick = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun BuildingCardNullablePreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_name" to "Name",
            "building_description" to "Description",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        BuildingCard(
            name = "Building",
        )
        BuildingCard(
            description = "Noice!"
        )
    }
}
