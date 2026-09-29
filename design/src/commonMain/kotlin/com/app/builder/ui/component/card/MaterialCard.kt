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
 * A material card.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param enabled Controls whether the input can accept user focus, selection gestures, or keystroke input updates.
 * @param selected The visual emphasis state of the card.
 * @param modifiedAt The modification timestamp of the material.
 * @param deletedAt The deletion timestamp of the material.
 * @param name The name of the material.
 * @param onNameChange Callback for when the name changes.
 * @param code The code of the material.
 * @param onCodeChange Callback for when the code changes.
 */
@Composable
fun MaterialCard(
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
                    title = "material_name",
                    value = it,
                    onValueChange = onNameChange
                )
            }
            code?.let {
                InputField(
                    enabled = enabled,
                    title = "material_code",
                    value = it,
                    onValueChange = onCodeChange
                )
            }
            modifiedAt?.let {
                InputField(
                    enabled = false,
                    title = "material_modified_at",
                    value = it,
                )
            }
            deletedAt?.let {
                InputField(
                    enabled = false,
                    title = "material_deleted_at",
                    value = it,
                )
            }
        }
    }
}

@Preview
@Composable
private fun MaterialCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "material_name" to "Name",
            "material_code" to "Code",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        MaterialCard(
            enabled = false,
            selected = false,
            name = "Material",
            code = "MATERIAL-001"
        )
        MaterialCard(
            enabled = true,
            selected = false,
            name = "Material",
            code = "MATERIAL-001"
        )
    }
}

@Preview
@Composable
private fun MaterialCardSelectedPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "material_name" to "Name",
            "material_code" to "Code",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        MaterialCard(
            enabled = true,
            selected = true,
            name = "Material",
            code = "MATERIAL-001"
        )
        MaterialCard(
            enabled = false,
            selected = true,
            name = "Material",
            code = "MATERIAL-001"
        )
    }
}

@Preview
@Composable
private fun MaterialCardNullablePreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "material_name" to "Name",
            "material_code" to "Code",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        MaterialCard(
            name = "Material",
        )
        MaterialCard(
            code = "MATERIAL-001"
        )
    }
}
