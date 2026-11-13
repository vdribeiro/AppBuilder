package com.app.builder.domain.gateway.account

import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission

/**
 * Validates that the account has the given [permission] for the given [entityType], reporting a denial through telemetry.
 *
 * @param tag The telemetry tag of the caller.
 * @param entityType The entity type being accessed.
 * @param permission The permission required to access the entity type.
 * @return The denial failure if the account lacks the permission, `null` otherwise.
 */
internal fun Account.validatePermission(tag: String, entityType: EntityType, permission: Permission): Throwable? {
    if (hasPermission(entityType = entityType, permission = permission)) return null

    val throwable = Throwable(message = "User does not have $permission permission for $entityType")
    Telemetry.error(tag = tag, message = "Invalid permissions", throwable = throwable)
    return throwable
}
