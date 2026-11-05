package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * Result of a successful authentication operation.
 *
 * @property user The authenticated user.
 * @property account The account the user authenticated with.
 * @property bearer The credentials used to authorize subsequent network requests.
 */
@Serializable
data class Authentication(
    val user: User,
    val account: Account,
    val bearer: BearerToken
)