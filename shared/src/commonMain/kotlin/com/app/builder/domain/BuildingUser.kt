package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuidOf

/**
 * Building user entity, linking a building and a user in a many-to-many relation.
 * Each building can be linked to many users, and each user to many buildings, with one building user per pair.
 *
 * @property modifiedAt The timestamp indicating when this building user was last updated.
 * @property deletedAt The timestamp indicating when this building user was last deleted.
 * @property buildingUuid The unique identifier of the linked building.
 * @property userUuid The unique identifier of the linked user.
 */
@Serializable
data class BuildingUser(
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val buildingUuid: Uuid,
    val userUuid: Uuid
) {
    /** The unique identifier of this building user, derived from [buildingUuid] and [userUuid] by [uuidOf]. */
    @Transient
    val uuid: Uuid = uuidOf(buildingUuid, userUuid)
}
