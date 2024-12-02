package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import com.app.builder.core.platform.OS

/**
 * A registry entry recorded for an incoming request, capturing client and device metadata sourced from the request interceptor.
 *
 * @property requestUuid The unique identifier of the request.
 * @property requestType The HTTP method used for the request.
 * @property requestUtc The UTC timestamp of when the request was made on the client.
 * @property requestSentUtc The UTC timestamp of when the request was sent by the client.
 * @property deletedAt The timestamp indicating when this registry was last deleted.
 * @property userUuid The unique identifier of the authenticated user.
 * @property appVersion The client application version.
 * @property os The operating system family of the client device.
 * @property osVersion The operating system version of the client device.
 * @property brand The manufacturer or brand of the client device.
 * @property model The hardware model of the client device.
 * @property deviceUuid The client device's installation UUID.
 * @property entityUuid The unique identifier of the entity targeted by the request, if any.
 * @property entityType The type of the entity targeted by the request, if any.
 * @property payload The request body, if captured.
 */
@Serializable
data class Registry(
    val requestUuid: Uuid,
    val requestType: String,
    val requestUtc: Instant,
    val requestSentUtc: Instant,
    val deletedAt: Instant?,
    val userUuid: Uuid,
    val appVersion: String,
    val os: OS,
    val osVersion: String,
    val brand: String,
    val model: String,
    val deviceUuid: Uuid,
    val entityUuid: Uuid?,
    val entityType: EntityType?,
    val payload: String?,
)
