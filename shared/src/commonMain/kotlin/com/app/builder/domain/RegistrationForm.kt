package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * Registration form creating an [Account] for a [User], storing the user too when it doesn't exist yet.
 *
 * @property user The user the account belongs to.
 * @property account The account to create, whose [Account.username] is the login username.
 * @property password The login password of the new account.
 */
@Serializable
data class RegistrationForm(
    val user: User,
    val account: Account,
    val password: String
) {
    /** The login credentials of the new account. */
    val credentials: UserCredentials get() = UserCredentials(username = account.username, password = password)

    /**
     * Returns a string representation of the form with the password masked.
     *
     * @return A sanitized string excluding the raw password value.
     */
    override fun toString(): String =
        "RegistrationForm(user=$user, account=$account, password=********)"
}