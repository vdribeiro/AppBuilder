package com.app.builder.domain

import kotlinx.serialization.Serializable

/** Permission types. */
@Serializable
enum class Permission {
    WRITE,
    READ
}

/**
 * Checks if these permissions grant a given [permission] for a given [entityType].
 * A [Permission.WRITE] permission also grants [Permission.READ].
 *
 * @receiver The permissions granted by entity type. An entity type that is not listed is not granted.
 * @param entityType The entity type to check.
 * @param permission The permission to check.
 * @return `true` if the permission is granted, `false` otherwise.
 */
fun Map<EntityType, Permission>.grants(entityType: EntityType, permission: Permission): Boolean = when (entityType) {
    EntityType.BUILDING_EQUIPMENT -> when (permission) {
        Permission.READ -> grants(entityType = EntityType.BUILDING, permission = Permission.READ)
        Permission.WRITE -> grants(entityType = EntityType.BUILDING, permission = Permission.WRITE) && grants(entityType = EntityType.EQUIPMENT, permission = Permission.READ)
    }
    else -> when (this[entityType]) {
        Permission.WRITE -> true
        Permission.READ -> permission == Permission.READ
        else -> false
    }
}
