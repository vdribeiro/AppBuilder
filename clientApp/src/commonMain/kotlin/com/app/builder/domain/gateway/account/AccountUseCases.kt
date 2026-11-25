package com.app.builder.domain.gateway.account

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import com.app.builder.domain.Account
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.gateway.authentication.PasswordResult

/** Defines the business workflows for accounts. */
interface AccountUseCases {

    /**
     * Observes the account of a specific user, fetching it from the server in the background.
     *
     * @param userUuid The unique identifier of the user whose account to observe.
     * @return [Flow] emitting the [Account] if found, or null if the user has no account.
     */
    fun observeAccount(userUuid: Uuid): Flow<Account?>

    /**
     * Creates an account for a user, which requires a connection since it carries the password.
     *
     * @param registrationForm The user, account and password to create the account with.
     * @return [PasswordResult.Success] with the created [Account], or why it wasn't created.
     */
    suspend fun createAccount(registrationForm: RegistrationForm): PasswordResult<Account>

    /**
     * Updates an existing account's username, permissions and disabled state, which requires a connection.
     *
     * @param account The [Account] to update.
     * @return The updated [Account], or null if the update failed.
     */
    suspend fun updateAccount(account: Account): Account?
}
