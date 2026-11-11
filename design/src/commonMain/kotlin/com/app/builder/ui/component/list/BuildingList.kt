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
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            EntityRow(
                modifier = Modifier
                    .testTag(tag = "building_row_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                deleted = item.deleted,
                headline = item.name,
                supporting = listOfNotNull(item.address, item.description).filter { it.isNotBlank() }.joinToString(separator = " · "),
                metadata = listOf(item.uuid.takeIf { item.uuidVisible }, item.modifiedAt, item.deletedAt)
            )
        }
    }
}

/**
 * Building item.
 *
 * @property uuid A unique identifier of the building. Defaults to a random UUID.
 * @property selected Whether the building is the one currently open.
 * @property deleted Whether the building is deleted.
 * @property uuidVisible Whether the UUID is shown on the row.
 * @property modifiedAt The timestamp indicating when this building was last updated.
 * @property deletedAt The timestamp indicating when this building was last deleted.
 * @property name The name of the building. Defaults to `null`.
 * @property description The description of the building. Defaults to `null`.
 * @property address The postal address of the building, shown before the description. Defaults to `null`.
 */
@Stable
data class BuildingItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val deleted: Boolean = false,
    val uuidVisible: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val name: String? = null,
    val description: String? = null,
    val address: String? = null
)

@PreviewLightDark
@Composable
private fun BuildingListPreview() = Preview {
    BuildingList(
        items = persistentListOf(
            BuildingItem(
                name = "Things"
            ),
            BuildingItem(
                name = "More things",
                description = "Shiny",
                address = "Avenida dos Aliados, Porto",
                selected = true
            ),
            BuildingItem(
                name = "Gone",
                deleted = true
            )
        )
    )
}