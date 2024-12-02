package com.app.builder.domain

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Represents a relationship linking a [Task] to a specific [User].
 *
 * @property taskUuid The unique identifier of the linked [Task].
 * @property userUuid The unique identifier of the linked [User].
 */
@Serializable
data class TaskUserJoin(
    val taskUuid: Uuid,
    val userUuid: Uuid,
)