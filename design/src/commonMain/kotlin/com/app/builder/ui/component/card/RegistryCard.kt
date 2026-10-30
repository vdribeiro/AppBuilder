package com.app.builder.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.text.InputField
import com.app.builder.ui.core.card.Card

/**
 * A read-only registry card, showing only the properties that are not `null`.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param selected The visual emphasis state of the card.
 * @param requestType The HTTP method of the request.
 * @param requestUtc When the request was made on the client.
 * @param requestSentUtc When the request was sent by the client.
 * @param deletedAt The deletion timestamp of the registry.
 * @param userUuid The UUID of the user that made the request.
 * @param appVersion The client application version.
 * @param os The operating system of the client device.
 * @param osVersion The operating system version of the client device.
 * @param brand The brand of the client device.
 * @param model The model of the client device.
 * @param deviceUuid The UUID of the device that made the request.
 * @param entityUuid The UUID of the entity targeted by the request.
 * @param entityType The type of the entity targeted by the request.
 * @param payload The request body.
 */
@Composable
fun RegistryCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    requestType: String? = null,
    requestUtc: String? = null,
    requestSentUtc: String? = null,
    deletedAt: String? = null,
    userUuid: String? = null,
    appVersion: String? = null,
    os: String? = null,
    osVersion: String? = null,
    brand: String? = null,
    model: String? = null,
    deviceUuid: String? = null,
    entityUuid: String? = null,
    entityType: String? = null,
    payload: String? = null,
) {
    val fields = listOf(
        "registry_request_type" to requestType,
        "registry_request_utc" to requestUtc,
        "registry_request_sent_utc" to requestSentUtc,
        "registry_deleted_at" to deletedAt,
        "registry_user_uuid" to userUuid,
        "registry_app_version" to appVersion,
        "registry_os" to os,
        "registry_os_version" to osVersion,
        "registry_brand" to brand,
        "registry_model" to model,
        "registry_device_uuid" to deviceUuid,
        "registry_entity_uuid" to entityUuid,
        "registry_entity_type" to entityType,
        "registry_payload" to payload,
    )

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
            fields.forEach { (title, value) ->
                value?.let {
                    InputField(
                        enabled = false,
                        title = title,
                        value = it,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RegistryCardPreview() = Preview {
    InjectTranslations(
        translations = mapOf(
            "registry_request_type" to "Request Type",
            "registry_entity_type" to "Entity Type",
            "registry_request_utc" to "Request At",
            "registry_os" to "OS",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        RegistryCard(
            selected = false,
            requestType = "POST",
            entityType = "TASK",
            requestUtc = "2026-10-30T10:00:00Z",
            os = "Android"
        )
        RegistryCard(
            selected = true,
            requestType = "GET",
            entityType = "USER",
        )
    }
}
