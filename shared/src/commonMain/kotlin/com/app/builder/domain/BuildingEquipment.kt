package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuidOf

/**
 * Building equipment entity, linking a building and an equipment in a many-to-many relation.
 * Each building can be linked to many equipments, and each equipment to many buildings, with one building equipment per pair.
 *
 * @property modifiedAt The timestamp indicating when this building equipment was last updated.
 * @property deletedAt The timestamp indicating when this building equipment was last deleted.
 * @property buildingUuid The unique identifier of the linked building.
 * @property equipmentUuid The unique identifier of the linked equipment.
 */
@Serializable
data class BuildingEquipment(
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val buildingUuid: Uuid,
    val equipmentUuid: Uuid
) {
    /** The unique identifier of this building equipment, derived from [buildingUuid] and [equipmentUuid] by [uuidOf]. */
    @Transient
    val uuid: Uuid = uuidOf(buildingUuid, equipmentUuid)
}
