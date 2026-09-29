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
import com.app.builder.ui.component.card.BuildingCard
import com.app.builder.ui.core.list.LazyColumn

/**
 * Building list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [BuildingItem]s.
 * @param onClick A callback invoked when an item is selected by the user.
 */
@Composable
fun BuildingList(
    modifier: Modifier = Modifier,
    items: ImmutableList<BuildingItem> = persistentListOf(),
    onClick: (BuildingItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "building_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(space = 2.dp)
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            BuildingCard(
                modifier = Modifier
                    .testTag(tag = "building_card_$index")
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
 * Building item.
 *
 * @property uuid A unique identifier of the building. Defaults to a random UUID.
 * @property selected The visual emphasis state of the card.
 * @property modifiedAt The timestamp indicating when this building was last updated.
 * @property deletedAt The timestamp indicating when this building was last deleted.
 * @property name The name of the building. Defaults to `null`.
 * @property code The code of the building. Defaults to `null`.
 */
@Stable
data class BuildingItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val name: String? = null,
    val code: String? = null
)

@Preview
@Composable
private fun BuildingListPreview() = Preview {
    BuildingList(
        items = persistentListOf(
            BuildingItem(
                name = "Building One"
            ),
            BuildingItem(
                name = "Building Two",
                code = "BUILDING-002"
            )
        )
    )
}
