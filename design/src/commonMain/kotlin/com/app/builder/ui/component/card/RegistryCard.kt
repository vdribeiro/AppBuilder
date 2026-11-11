package com.app.builder.ui.component.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.InjectTranslations
import com.app.builder.ui.Preview
import com.app.builder.ui.component.field.FieldValue
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.divider.Divider

/**
 * A read-only registry card, showing only the properties that are not `null`, grouped into the request, the client that made it, and its body.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param requestUuid The UUID of the request.
 * @param requestType The HTTP method of the request.
 * @param requestUtc When the request was made on the client.
 * @param requestSentUtc When the request was sent by the client.
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
    requestUuid: String? = null,
    requestType: String? = null,
    requestUtc: String? = null,
    requestSentUtc: String? = null,
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
    val groups = listOf(
        listOf(
            "registry_request_type" to requestType,
            "registry_entity_type" to entityType,
            "registry_entity_uuid" to entityUuid,
            "registry_request_utc" to requestUtc,
            "registry_request_sent_utc" to requestSentUtc,
        ),
        listOf(
            "registry_request_uuid" to requestUuid,
            "registry_user_uuid" to userUuid,
            "registry_device_uuid" to deviceUuid,
            "registry_brand" to brand,
            "registry_model" to model,
            "registry_os" to os,
            "registry_os_version" to osVersion,
            "registry_app_version" to appVersion,
        ),
        listOf(
            "registry_payload" to payload,
        ),
    ).map { group -> group.filter { it.second != null } }.filter { it.isNotEmpty() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            groups.forEachIndexed { index, group ->
                if (index > 0) Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                group.forEach { (label, value) -> FieldValue(label = label, value = value) }
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
            "registry_payload" to "Payload",
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        RegistryCard(
            requestType = "POST",
            entityType = "TASK",
            requestUtc = "30/10/2026, 10:00",
            os = "Android",
            payload = "{\"title\":\"Task\"}"
        )
        RegistryCard(
            requestType = "GET",
            entityType = "USER",
        )
    }
}
