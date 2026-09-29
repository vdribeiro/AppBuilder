package com.app.builder.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.text.InputField
import com.app.builder.ui.core.card.Card

/**
 * A building card.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Controls whether the input can accept user focus, selection gestures, or keystroke input updates.
 * @param selected The visual emphasis state of the card.
 * @param modifiedAt The modification timestamp of the building.
 * @param deletedAt The deletion timestamp of the building.
 * @param name The name of the building.
 * @param onNameChange Callback for when the name changes.
 * @param code The code of the building.
 * @param onCodeChange Callback for when the code changes.
 */
@Composable
fun BuildingCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    selected: Boolean = false,
    modifiedAt: String? = null,
    deletedAt: String? = null,
    name: String? = null,
    onNameChange: (String) -> Unit = {},
    code: String? = null,
    onCodeChange: (String) -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
        selected = selected
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.Top),
            horizontalAlignment = Alignment.Start,
        ) {
            name?.let {
                InputField(
                    enabled = enabled,
                    title = "building_name",
                    value = it,
                    onValueChange = onNameChange
                )
            }
            code?.let {
                InputField(
                    enabled = enabled,
                    title = "building_code",
                    value = it,
                    onValueChange = onCodeChange
                )
            }
            modifiedAt?.let {
                InputField(
                    enabled = false,
                    title = "building_modified_at",
                    value = it,
                )
            }
            deletedAt?.let {
                InputField(
                    enabled = false,
                    title = "building_deleted_at",
                    value = it,
                )
            }
        }
    }
}

@Preview
@Composable
private fun BuildingCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_name" to "Name",
            "building_code" to "Code",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        BuildingCard(
            enabled = false,
            selected = false,
            name = "Building",
            code = "BUILDING-001"
        )
        BuildingCard(
            enabled = true,
            selected = false,
            name = "Building",
            code = "BUILDING-001"
        )
    }
}

@Preview
@Composable
private fun BuildingCardSelectedPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_name" to "Name",
            "building_code" to "Code",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        BuildingCard(
            enabled = true,
            selected = true,
            name = "Building",
            code = "BUILDING-001"
        )
        BuildingCard(
            enabled = false,
            selected = true,
            name = "Building",
            code = "BUILDING-001"
        )
    }
}

@Preview
@Composable
private fun BuildingCardNullablePreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "building_name" to "Name",
            "building_code" to "Code",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        BuildingCard(
            name = "Building",
        )
        BuildingCard(
            code = "BUILDING-001"
        )
    }
}
