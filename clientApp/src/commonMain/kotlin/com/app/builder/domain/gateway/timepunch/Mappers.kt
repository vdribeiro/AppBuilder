package com.app.builder.domain.gateway.timepunch

import com.app.builder.data.database.TimePunchSchema
import com.app.builder.domain.TimePunch

/**
 * Maps a [TimePunchSchema] entity to a [TimePunch] domain model.
 *
 * @return The corresponding [TimePunch] instance.
 */
fun TimePunchSchema.toTimePunch(): TimePunch = TimePunch(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    userUuid = userUuid,
    time = time,
    source = source
)

/**
 * Maps a [TimePunch] domain model to a [TimePunchSchema] entity.
 *
 * @return The corresponding [TimePunchSchema] instance.
 */
fun TimePunch.toTimePunchSchema(): TimePunchSchema = TimePunchSchema(
    uuid = uuid,
    modifiedAt = modifiedAt,
    deletedAt = deletedAt,
    userUuid = userUuid,
    time = time,
    source = source
)
