package com.app.builder.domain.gateway.registry

import com.app.builder.core.platform.OS
import com.app.builder.data.database.RegistrySchema
import com.app.builder.domain.Registry

/**
 * Maps a [RegistrySchema] entity to a [Registry] domain model.
 *
 * @return The corresponding [Registry] instance.
 */
fun RegistrySchema.toRegistry(): Registry = Registry(
    requestUuid = requestUuid,
    requestType = requestType,
    requestUtc = requestUtc,
    requestSentUtc = requestSentUtc,
    deletedAt = deletedAt,
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
    deletedAt = deletedAt,
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
