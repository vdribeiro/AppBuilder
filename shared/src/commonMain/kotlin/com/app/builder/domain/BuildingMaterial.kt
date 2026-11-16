package com.app.builder.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuidOf

/**
 * Building material entity, linking a building and a material in a many-to-many relation.
 * Each building can store many materials, and each material can be stored in many buildings, with one building material per pair.
 *
 * @property modifiedAt The timestamp indicating when this building material was last updated.
 * @property deletedAt The timestamp indicating when this building material was last deleted.
 * @property buildingUuid The unique identifier of the linked building.
 * @property materialUuid The unique identifier of the linked material.
 * @property quantity The quantity of the material stored in the building.
 */
@Serializable
data class BuildingMaterial(
    val modifiedAt: Instant = now(),
    val deletedAt: Instant? = null,
    val buildingUuid: Uuid,
    val materialUuid: Uuid,
    val quantity: Int = 1
) {
    /** The unique identifier of this building material, derived from [buildingUuid] and [materialUuid] by [uuidOf]. */
    @Transient
    val uuid: Uuid = uuidOf(buildingUuid, materialUuid)
}
