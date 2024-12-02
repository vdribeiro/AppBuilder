package com.app.builder.domain

import kotlinx.serialization.Serializable

/**
 * Result of a successful authentication operation.
 *
 * @property user The authenticated user.
 * @property bearer The credentials used to authorize subsequent network requests.
 */
@Serializable
data class Authentication(
    val user: User,
    val bearer: BearerToken
)