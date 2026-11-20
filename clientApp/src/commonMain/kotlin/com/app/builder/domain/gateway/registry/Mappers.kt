package com.app.builder.domain.gateway.registry

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.platform.OS
import com.app.builder.data.database.RegistrySchema
import com.app.builder.domain.EntityType
import com.app.builder.domain.Registry

/**
 * Maps the columns of a registry row joined with its user to a [RegistryEntry].
 *
 * @param requestUuid The unique identifier of the request.
 * @param requestType The HTTP method used for the request.
 * @param requestUtc The UTC timestamp of when the request was made on the client.
 * @param requestSentUtc The UTC timestamp of when the request was sent by the client.
 * @param userUuid The unique identifier of the authenticated user.
 * @param appVersion The client application version.
 * @param os The operating system family of the client device.
 * @param osVersion The operating system version of the client device.
 * @param brand The manufacturer or brand of the client device.
 * @param model The hardware model of the client device.
 * @param deviceUuid The client device's installation UUID.
 * @param entityUuid The unique identifier of the entity targeted by the request, if any.
 * @param entityType The type of the entity targeted by the request, if any.
 * @param payload The request body, if captured.
 * @param userName The name of the joined user, if any.
 * @return The corresponding [RegistryEntry] instance.
 */
fun toRegistryEntry(
    requestUuid: Uuid,
    requestType: String,
    requestUtc: Instant,
    requestSentUtc: Instant,
    userUuid: Uuid,
    appVersion: String,
    os: String,
    osVersion: String,
    brand: String,
    model: String,
    deviceUuid: Uuid,
    entityUuid: Uuid?,
    entityType: EntityType?,
    payload: String?,
    userName: String?,
): RegistryEntry = RegistryEntry(
    registry = Registry(
        requestUuid = requestUuid,
        requestType = requestType,
        requestUtc = requestUtc,
        requestSentUtc = requestSentUtc,
        userUuid = userUuid,
        appVersion = appVersion,
        os = OS.fromString(value = os),
        osVersion = osVersion,
        brand = brand,
        model = model,
        deviceUuid = deviceUuid,
        entityUuid = entityUuid,
        entityType = entityType,
        payload = payload
    ),
    userName = userName
)

/**
 * Maps a [Registry] domain model to a [RegistrySchema] entity.
 *
 * @return The corresponding [RegistrySchema] instance.
 */
fun Registry.toRegistrySchema(): RegistrySchema = RegistrySchema(
    requestUuid = requestUuid,
    requestType = requestType,
    requestUtc = requestUtc,
    requestSentUtc = requestSentUtc,
    userUuid = userUuid,
    appVersion = appVersion,
    os = os.toString(),
    osVersion = osVersion,
    brand = brand,
    model = model,
    deviceUuid = deviceUuid,
    entityUuid = entityUuid,
    entityType = entityType,
    payload = payload
)
