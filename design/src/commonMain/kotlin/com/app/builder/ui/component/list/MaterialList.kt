package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.core.list.LazyColumn

/**
 * Material list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [MaterialItem]s.
 * @param onClick A callback invoked when an item is selected by the user.
 */
@Composable
fun MaterialList(
    modifier: Modifier = Modifier,
    items: ImmutableList<MaterialItem> = persistentListOf(),
    onClick: (MaterialItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "material_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            EntityRow(
                modifier = Modifier
                    .testTag(tag = "material_row_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                deleted = item.deleted,
                checked = item.checked,
                headline = item.name,
                supporting = item.description,
                metadata = listOf(
                    EntityRowMetadata(text = item.uuid.takeIf { item.uuidVisible }),
                    EntityRowMetadata(text = item.modifiedAt, dateTime = true),
                    EntityRowMetadata(text = item.deletedAt, dateTime = true),
                )
            )
        }
    }
}

/**
 * Material item.
 *
 * @property uuid A unique identifier of the material. Defaults to a random UUID.
 * @property selected Whether the material is the one currently open.
 * @property deleted Whether the material is deleted.
 * @property checked Whether the material is ticked in a picker, or `null` outside a picker.
 * @property uuidVisible Whether the UUID is shown on the row.
 * @property modifiedAt The timestamp indicating when this material was last updated, as a UTC ISO-8601 string shown as a localized date-time.
 * @property deletedAt The timestamp indicating when this material was last deleted, as a UTC ISO-8601 string shown as a localized date-time.
 * @property name The name of the material. Defaults to `null`.
 * @property description The description of the material. Defaults to `null`.
 */
@Stable
data class MaterialItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val deleted: Boolean = false,
    val checked: Boolean? = null,
    val uuidVisible: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val name: String? = null,
    val description: String? = null
)

@PreviewLightDark
@Composable
private fun MaterialListPreview() = Preview {
    MaterialList(
        items = persistentListOf(
            MaterialItem(
                name = "Copper Cable"
            ),
            MaterialItem(
                name = "Cement",
                description = "25 kg bags",
                selected = true
            ),
            MaterialItem(
                name = "Gone",
                deleted = true
            )
        )
    )
}