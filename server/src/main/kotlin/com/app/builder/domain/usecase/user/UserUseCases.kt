package com.app.builder.domain.usecase.user

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.User
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing users. */
interface UserUseCases {

    /**
     * Gets a page of users updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last user consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The UUID of the last user consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return [Outcome.Success] with the [User] objects, or [Outcome.Error] when the operation failed.
     */
    suspend fun getUsers(lastSyncUtc: Instant, cursorUtc: Instant?, cursorUuid: Uuid?, pageSize: Int): Outcome<List<User>>

    /**
     * Gets a single user by UUID.
     *
     * @param uuid The user's unique identifier.
     * @return [Outcome.Success] with the [User], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getUser(uuid: Uuid): Outcome<User>

    /**
     * Inserts a new user or updates an existing one.
     *
     * @param user The [user] to be inserted or updated.
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertUser(user: User): Outcome<Unit>
}
