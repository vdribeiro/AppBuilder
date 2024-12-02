package com.app.builder.domain.scheduler

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType

/**
 * Represents a persistent unit of background work managed by the scheduling subsystem.
 * Models the execution state, retry counters, and conflict resolution behaviors required to process asynchronous tasks.
 *
 * @property uuid The unique identifier tracking the job execution node.
 * @property userUuid The unique identifier of the user that originated the request.
 * @property utc The timestamp marking when the job was instantiated.
 * @property entityUuid The unique UUID of the entity being mutated.
 * @property entityType The domain type of the entity.
 * @property type The high-level operational intent of a background job.
 * @property conflictPolicy The strategy dictating how to handle scheduling conflicts if a duplicate job type is detected.
 * @property payload A JSON string representing the execution parameters required by the task.
 * @property state The current lifecycle status of the execution request.
 * @property attempt A zero-indexed counter tracking retry iterations in failure-recovery scenarios.
 */
data class Job(
    val uuid: Uuid = uuid(),
    val userUuid: Uuid,
    val utc: Instant = now(),
    val entityUuid: Uuid? = null,
    val entityType: EntityType,
    val type: Type,
    val conflictPolicy: ConflictPolicy = when (type) {
        Type.GET -> ConflictPolicy.IGNORE
        Type.POST, Type.DELETE -> ConflictPolicy.APPEND
    },
    val payload: String? = null,
    val state: State = State.PENDING,
    val attempt: Int = 0,
) {
    /** Defines the high-level operational intent of a background job. */
    enum class Type {
        GET,
        POST,
        DELETE
    }

    /** The execution lifecycle states of a background job. */
    enum class State {
        /** Registered in storage and awaiting an execution slot. */
        PENDING,
        /** Acquired an active execution lock and is currently processing. */
        RUNNING,
        /** Business logic completed successfully. */
        COMPLETE,
        /** Operation failed permanently or exceeded retry thresholds. */
        FAILED,
        /** The job was aborted. */
        CANCELED;
    }

    /** Determines how the scheduler handles queuing collisions when a duplicate job type is scheduled. */
    enum class ConflictPolicy {
        /** Add the incoming job to the execution queue normally, ignoring duplicates. */
        APPEND,
        /** Evict any prior pending duplicate jobs from the queue before enqueuing the new task. */
        REPLACE,
        /** Ignore the new job if a duplicate is already in the queue. */
        IGNORE
    }

    /**
     * Generates a string representation of the job, stripping out potentially large or sensitive contents within the [payload] property.
     *
     * @return A sanitized structural string representing the job.
     */
    override fun toString(): String {
        return "Job(uuid='$uuid', userUuid='$userUuid', utc='$utc', type=$type, conflictPolicy=$conflictPolicy, state=$state, attempt=$attempt)"
    }
}
