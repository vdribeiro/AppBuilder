package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.list.ListItem

/**
 * File list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [DeviceFileItem]s.
 * @param onClick A callback invoked when an item is selected by the user.
 */
@Composable
fun DeviceFileList(
    modifier: Modifier = Modifier,
    items: ImmutableList<DeviceFileItem> = persistentListOf(),
    onClick: (DeviceFileItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "file_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start
    ) {
        itemsIndexed(items = items, key = { _, item -> item.path }) { index, item ->
            ListItem(
                modifier = Modifier
                    .testTag(tag = "file_item_$index")
                    .clickable { onClick(item) },
                headlineText = item.name,
                supportingText = item.description,
                leadingContent = { Icon(imageVector = Icons.Filled.InsertDriveFile) }
            )
        }
    }
}

/**
 * File item.
 *
 * @property path The absolute path of the file, used as the item's identity.
 * @property name The file name shown as the headline.
 * @property description The already formatted size and modification date of the file. Defaults to `null`.
 */
@Stable
data class DeviceFileItem(
    val path: String,
    val name: String,
    val description: String? = null
)

@Preview
@Composable
private fun DeviceFileListPreview() = Preview {
    DeviceFileList(
        items = persistentListOf(
            DeviceFileItem(
                path = "/cache/photo_1.jpg",
                name = "photo_1.jpg",
                description = "1.2 MB"
            ),
            DeviceFileItem(
                path = "/cache/video_1.mp4",
                name = "video_1.mp4"
            )
        )
    )
}
