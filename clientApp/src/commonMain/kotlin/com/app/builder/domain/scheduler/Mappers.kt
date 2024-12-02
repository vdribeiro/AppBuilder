package com.app.builder.domain.scheduler

import com.app.builder.data.database.JobSchema
import com.app.builder.data.http.Header

/**
 * Maps a [JobSchema] entity to a [Job] domain model.
 *
 * @return The corresponding [Job] instance.
 */
fun JobSchema.toJob(): Job = Job(
    uuid = uuid,
    userUuid = userUuid,
    utc = utc,
    entityUuid = entityUuid,
    entityType = entityType,
    type = type,
    conflictPolicy = conflictPolicy,
    payload = payload,
    state = state,
    attempt = attempt,
)

/**
 * Maps a [Job] domain model to a [JobSchema] entity.
 *
 * @return The corresponding [JobSchema] instance.
 */
fun Job.toJobSchema(): JobSchema = JobSchema(
    uuid = uuid,
    userUuid = userUuid,
    utc = utc,
    entityUuid = entityUuid,
    entityType = entityType,
    type = type,
    conflictPolicy = conflictPolicy,
    payload = payload,
    state = state,
    attempt = attempt,
)

/**
 * Builds the set of request headers used to identify a [Job] to the server.
 *
 * @return A map of [Header]s carrying the job's unique request UUID and UTC timestamp.
 */
fun Job.toHeaderMap(): Map<Header, String> = mapOf(
    Header.RequestUuid to uuid.toString(),
    Header.RequestUtc to utc.toString()
)
