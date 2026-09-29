package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.component.card.EquipmentCard
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
        verticalArrangement = Arrangement.spacedBy(space = 2.dp)
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            EquipmentCard(
                modifier = Modifier
                    .testTag(tag = "equipment_card_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                modifiedAt = item.modifiedAt,
                deletedAt = item.deletedAt,
                name = item.name,
                code = item.code,
            )
        }
    }
}

/**
 * Equipment item.
 *
 * @property uuid A unique identifier of the equipment. Defaults to a random UUID.
 * @property selected The visual emphasis state of the card.
 * @property modifiedAt The timestamp indicating when this equipment was last updated.
 * @property deletedAt The timestamp indicating when this equipment was last deleted.
 * @property name The name of the equipment. Defaults to `null`.
 * @property code The code of the equipment. Defaults to `null`.
 */
@Stable
data class EquipmentItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val name: String? = null,
    val code: String? = null
)

@Preview
@Composable
private fun EquipmentListPreview() = Preview {
    EquipmentList(
        items = persistentListOf(
            EquipmentItem(
                name = "Equipment One"
            ),
            EquipmentItem(
                name = "Equipment Two",
                code = "EQUIPMENT-002"
            )
        )
    )
}
