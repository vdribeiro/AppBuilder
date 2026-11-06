package com.app.builder.domain.usecase.account

import kotlin.time.Instant
import kotlin.uuid.Uuid
import com.app.builder.core.config.ServerConfigs
import com.app.builder.domain.Account
import com.app.builder.domain.usecase.WriteResult

/** Defines the business workflows for managing accounts. */
interface AccountUseCases {

    /**
     * Gets a page of accounts updated after the given timestamp, newest first.
     *
     * @param lastSyncUtc The fixed lower bound below which results are already synced.
     * @param cursorUtc The `modifiedAt` of the last account consumed in the previous page, for descending cursor continuation.
     * @param cursorUuid The user UUID of the last account consumed in the previous page, tie-breaking [cursorUtc].
     * @param pageSize The maximum number of entries to return, clamped to [ServerConfigs.maxPageSize].
     * @return The list of [Account] objects.
     */
    suspend fun getAccounts(lastSyncUtc: Instant, cursorUtc: Instant?, cursorUuid: Uuid?, pageSize: Int): List<Account>

    /**
     * Gets the account of a single user.
     *
     * @param userUuid The unique identifier of the user the account belongs to.
     * @return The [Account], or null if an error occurred or the user has no account.
     */
    suspend fun getAccount(userUuid: Uuid): Account?

    /**
     * Updates the username, permissions and deleted state of an existing account, leaving its password unchanged.
     * When the account is deleted, the device tokens of its user are removed too.
     *
     * @param account The [account] to update.
     * @return [WriteResult.SUCCESS] when updated, [WriteResult.NOT_FOUND] when the user has no account, [WriteResult.CONFLICT] when another account has the username, or [WriteResult.ERROR] when the operation failed.
     */
    suspend fun upsertAccount(account: Account): WriteResult
}
