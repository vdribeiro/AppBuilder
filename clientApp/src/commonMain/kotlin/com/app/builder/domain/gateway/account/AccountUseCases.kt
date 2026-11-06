package com.app.builder.domain.gateway.account

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Account
import com.app.builder.domain.RegistrationForm

/** Defines the business workflows for accounts. */
interface AccountUseCases {

    /**
     * Observes the complete list of accounts.
     *
     * @return [Flow] emitting the current list of [Account] objects.
     */
    fun observeAccounts(): Flow<List<Account>>

    /**
     * Observes the account of a specific user.
     *
     * @param userUuid The unique identifier of the user whose account to observe.
     * @return [Flow] emitting the [Account] if found, or null if the user has no account.
     */
    fun observeAccount(userUuid: Uuid): Flow<Account?>

    /**
     * Creates an account for a user, which requires a connection since it carries the password.
     *
     * @param registrationForm The user, account and password to create the account with.
     * @return The created [Account], or null if the creation failed.
     */
    suspend fun createAccount(registrationForm: RegistrationForm): Account?

    /**
     * Updates an existing account's username, permissions and disabled state, which requires a connection.
     *
     * @param account The [Account] to update.
     * @return The updated [Account], or null if the update failed.
     */
    suspend fun updateAccount(account: Account): Account?
}
