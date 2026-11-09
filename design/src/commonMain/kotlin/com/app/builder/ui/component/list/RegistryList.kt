package com.app.builder.ui.component.list

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.LazyColumn

/**
 * Registry list.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param items An [ImmutableList] of [RegistryItem]s.
 * @param onClick A callback invoked when an item is selected by the user.
 */
@Composable
fun RegistryList(
    modifier: Modifier = Modifier,
    items: ImmutableList<RegistryItem> = persistentListOf(),
    onClick: (RegistryItem) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier
            .testTag(tag = "registry_list")
            .fillMaxSize(),
        horizontalAlignment = Alignment.Start,
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            EntityRow(
                modifier = Modifier
                    .testTag(tag = "registry_row_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                headline = listOfNotNull(item.requestType, item.entityType).joinToString(separator = " "),
                supporting = listOfNotNull(
                    listOfNotNull(item.brand, item.model, item.os, item.osVersion, item.appVersion).joinToString(separator = " · ").takeIf { it.isNotEmpty() },
                    item.payload
                ).joinToString(separator = "\n"),
                metadata = listOf(
                    item.uuid.takeIf { item.uuidVisible },
                    item.requestUtc,
                    item.requestSentUtc,
                    item.userUuid,
                    item.deviceUuid,
                    item.entityUuid,
                ),
                leadingContent = { Icon(imageVector = requestIcon(requestType = item.requestType)) }
            )
        }
    }
}

/**
 * Picks the icon of a request from its HTTP method.
 *
 * @param requestType The HTTP method of the request, or `null` when hidden.
 * @return The icon for creating, editing or deleting, or a generic one for any other method.
 */
private fun requestIcon(requestType: String?): ImageVector = when (requestType?.uppercase()) {
    "POST" -> Icons.Outlined.AddCircleOutline
    "PUT", "PATCH" -> Icons.Outlined.Edit
    "DELETE" -> Icons.Outlined.Delete
    else -> Icons.Outlined.SwapVert
}

/**
 * Registry item, where a `null` property is hidden.
 *
 * @property uuid The request UUID of the registry. Defaults to a random UUID.
 * @property selected Whether the registry is the one currently open.
 * @property uuidVisible Whether the request UUID is shown on the row.
 * @property requestType The HTTP method of the request.
 * @property requestUtc When the request was made on the client.
 * @property requestSentUtc When the request was sent by the client.
 * @property userUuid The UUID of the user that made the request.
 * @property appVersion The client application version.
 * @property os The operating system of the client device.
 * @property osVersion The operating system version of the client device.
 * @property brand The brand of the client device.
 * @property model The model of the client device.
 * @property deviceUuid The UUID of the device that made the request.
 * @property entityUuid The UUID of the entity targeted by the request.
 * @property entityType The type of the entity targeted by the request.
 * @property payload The request body.
 */
@Stable
data class RegistryItem(
    val uuid: String = uuid().toString(),
    val selected: Boolean = false,
    val uuidVisible: Boolean = false,
    val requestType: String? = null,
    val requestUtc: String? = null,
    val requestSentUtc: String? = null,
    val userUuid: String? = null,
    val appVersion: String? = null,
    val os: String? = null,
    val osVersion: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val deviceUuid: String? = null,
    val entityUuid: String? = null,
    val entityType: String? = null,
    val payload: String? = null,
)

@PreviewLightDark
@Composable
private fun RegistryListPreview() = Preview {
    RegistryList(
        items = persistentListOf(
            RegistryItem(
                requestType = "POST",
                entityType = "TASK"
            ),
            RegistryItem(
                selected = true,
                requestType = "PUT",
                entityType = "USER",
                os = "Android",
                payload = "{\"name\":\"Jane\"}"
            )
        )
    )
}
