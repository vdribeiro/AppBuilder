package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * Registration form containing the user profile information and login credentials required to create an account.
 *
 * @property user The user profile data.
 * @property credentials The security credentials for the new account.
 */
@Serializable
data class RegistrationForm(
    val user: User,
    val credentials: UserCredentials
)