package com.app.builder.domain.usecase.account

import kotlin.uuid.Uuid
import com.app.builder.domain.Account
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing accounts. */
interface AccountUseCases {

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
     * @return [Outcome.Success] when updated, [Outcome.NotFound] when the user has no account, [Outcome.Conflict] when another account has the username, or [Outcome.Error] when the operation failed.
     */
    suspend fun upsertAccount(account: Account): Outcome<Unit>
}
