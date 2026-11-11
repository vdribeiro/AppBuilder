package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.LazyColumn

/**
 * Equipment list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [EquipmentItem]s.
 * @param onClick A callback invoked when an item is selected by the user.
 */
@Composable
fun EquipmentList(
    modifier: Modifier = Modifier,
    items: ImmutableList<EquipmentItem> = persistentListOf(),
    onClick: (EquipmentItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "equipment_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            EntityRow(
                modifier = Modifier
                    .testTag(tag = "equipment_row_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                deleted = item.deleted,
                headline = item.name,
                supporting = item.description,
                metadata = listOf(item.uuid.takeIf { item.uuidVisible }, item.modifiedAt, item.deletedAt),
                leadingContent = item.malfunctioning?.let { malfunctioning -> { Icon(imageVector = if (malfunctioning) Icons.Default.Error else Icons.Default.CheckCircle) } }
            )
        }
    }
}

/**
 * Equipment item.
 *
 * @property uuid A unique identifier of the equipment. Defaults to a random UUID.
 * @property selected Whether the equipment is the one currently open.
 * @property deleted Whether the equipment is deleted.
 * @property uuidVisible Whether the UUID is shown on the row.
 * @property modifiedAt The timestamp indicating when this equipment was last updated.
 * @property deletedAt The timestamp indicating when this equipment was last deleted.
 * @property name The name of the equipment. Defaults to `null`.
 * @property description The description of the equipment. Defaults to `null`.
 * @property malfunctioning Whether the equipment is malfunctioning, or `null` to hide its status. Defaults to `null`.
 */
@Stable
data class EquipmentItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val deleted: Boolean = false,
    val uuidVisible: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val name: String? = null,
    val description: String? = null,
    val malfunctioning: Boolean? = null
)

@PreviewLightDark
@Composable
private fun EquipmentListPreview() = Preview {
    EquipmentList(
        items = persistentListOf(
            EquipmentItem(
                name = "Things"
            ),
            EquipmentItem(
                name = "More things",
                description = "Shiny",
                malfunctioning = true,
                selected = true
            ),
            EquipmentItem(
                name = "Gone",
                deleted = true
            )
        )
    )
}