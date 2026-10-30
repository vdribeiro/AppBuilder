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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.core.security.uuid
import com.app.builder.ui.Preview
import com.app.builder.ui.component.card.RegistryCard
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
        verticalArrangement = Arrangement.spacedBy(space = 2.dp)
    ) {
        itemsIndexed(items = items, key = { _, item -> item.uuid }) { index, item ->
            RegistryCard(
                modifier = Modifier
                    .testTag(tag = "registry_card_$index")
                    .clickable { onClick(item) },
                selected = item.selected,
                requestType = item.requestType,
                requestUtc = item.requestUtc,
                requestSentUtc = item.requestSentUtc,
                deletedAt = item.deletedAt,
                userUuid = item.userUuid,
                appVersion = item.appVersion,
                os = item.os,
                osVersion = item.osVersion,
                brand = item.brand,
                model = item.model,
                deviceUuid = item.deviceUuid,
                entityUuid = item.entityUuid,
                entityType = item.entityType,
                payload = item.payload,
            )
        }
    }
}

/**
 * Registry item, where a `null` property is hidden.
 *
 * @property uuid The request UUID of the registry. Defaults to a random UUID.
 * @property selected The visual emphasis state of the card.
 * @property requestType The HTTP method of the request.
 * @property requestUtc When the request was made on the client.
 * @property requestSentUtc When the request was sent by the client.
 * @property deletedAt The deletion timestamp of the registry.
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
    val requestType: String? = null,
    val requestUtc: String? = null,
    val requestSentUtc: String? = null,
    val deletedAt: String? = null,
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
                requestType = "GET",
                entityType = "USER",
                os = "Android"
            )
        )
    )
}
