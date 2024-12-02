package com.app.builder.domain

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * FCM token.
 *
 * @property deviceUuid The unique identifier of the device.
 * @property token The FCM registration token for this device.
 */
@Serializable
data class DeviceToken(
    val deviceUuid: Uuid? = null,
    val token: String,
)