package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.TouchApp
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
import com.app.builder.ui.getDateTime

/**
 * Time punch list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [TimePunchItem]s.
 * @param onClick A callback invoked when an item is selected by the user.
 */
@Composable
fun TimePunchList(
    modifier: Modifier = Modifier,
    items: ImmutableList<TimePunchItem> = persistentListOf(),
    onClick: (TimePunchItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "time_punch_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            EntityRow(
                modifier = Modifier
                    .testTag(tag = "time_punch_row_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                deleted = item.deleted,
                headline = item.time?.let { getDateTime(utc = it) },
                supporting = item.userName,
                metadata = listOf(
                    item.uuid.takeIf { item.uuidVisible },
                    item.userUuid,
                    item.modifiedAt?.let { getDateTime(utc = it) },
                    item.deletedAt?.let { getDateTime(utc = it) },
                ),
                leadingContent = item.nfc?.let { nfc -> { Icon(imageVector = if (nfc) Icons.Default.Nfc else Icons.Default.TouchApp) } }
            )
        }
    }
}

/**
 * Time punch item.
 *
 * @property uuid A unique identifier of the time punch. Defaults to a random UUID.
 * @property selected Whether the time punch is the one currently open.
 * @property deleted Whether the time punch is deleted.
 * @property uuidVisible Whether the UUID is shown on the row.
 * @property modifiedAt The timestamp indicating when this time punch was last updated, as a UTC ISO-8601 string shown as a localized date-time.
 * @property deletedAt The timestamp indicating when this time punch was last deleted, as a UTC ISO-8601 string shown as a localized date-time.
 * @property time The time at which the user punched the clock, as a UTC ISO-8601 string shown as a localized date-time. Defaults to `null`.
 * @property userUuid The UUID of the user that punched the clock. Defaults to `null`.
 * @property userName The name of the user that punched the clock. Defaults to `null`.
 * @property nfc Whether the time punch came from an NFC tag, or `null` to hide its source. Defaults to `null`.
 */
@Stable
data class TimePunchItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val deleted: Boolean = false,
    val uuidVisible: Boolean = false,
    val modifiedAt: String? = null,
    val deletedAt: String? = null,
    val time: String? = null,
    val userUuid: String? = null,
    val userName: String? = null,
    val nfc: Boolean? = null
)

@PreviewLightDark
@Composable
private fun TimePunchListPreview() = Preview {
    TimePunchList(
        items = persistentListOf(
            TimePunchItem(
                time = "2026-11-09T09:00:00Z",
                userName = "Vitor Ribeiro"
            ),
            TimePunchItem(
                time = "2026-11-09T18:00:00Z",
                userName = "Vitor Ribeiro",
                nfc = true,
                selected = true
            ),
            TimePunchItem(
                time = "2026-11-10T09:00:00Z",
                userName = "Jane Doe",
                nfc = false,
                deleted = true
            )
        )
    )
}
