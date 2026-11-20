package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuidOf

/**
 * Equipment user entity, linking an equipment and a user in a many-to-many relation.
 * Each equipment can be linked to many users, and each user to many equipments, with one equipment user per pair.
 *
 * @property modifiedAt The timestamp indicating when this equipment user was last updated.
 * @property deletedAt The timestamp indicating when this equipment user was last deleted.
 * @property equipmentUuid The unique identifier of the linked equipment.
 * @property userUuid The unique identifier of the linked user.
 */
@Serializable
data class EquipmentUser(
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val equipmentUuid: Uuid,
    val userUuid: Uuid
) {
    /** The unique identifier of this equipment user, derived from [equipmentUuid] and [userUuid] by [uuidOf]. */
    @Transient
    val uuid: Uuid = uuidOf(equipmentUuid, userUuid)
}
