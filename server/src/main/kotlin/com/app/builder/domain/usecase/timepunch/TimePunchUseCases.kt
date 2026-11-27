package com.app.builder.domain.usecase.timepunch

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.TimePunch
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing time punches. */
interface TimePunchUseCases {

    /**
     * Gets a page of time punches updated after the given timestamp, newest first.
     *
     * @param userUuid The user whose time punches are returned, or `null` to return the time punches of every user.
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last time punch consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last time punch consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [TimePunch] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getTimePunches(
        userUuid: Uuid?,
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): Outcome<List<TimePunch>>

    /**
     * Gets a single time punch by UUID.
     *
     * @param uuid The time punch's unique identifier.
     * @return [Outcome.Success] with the [TimePunch], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getTimePunch(uuid: Uuid): Outcome<TimePunch>

    /**
     * Inserts a new time punch or updates an existing one.
     *
     * @param timePunch The [TimePunch] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertTimePunch(timePunch: TimePunch): Outcome<Unit>
}
