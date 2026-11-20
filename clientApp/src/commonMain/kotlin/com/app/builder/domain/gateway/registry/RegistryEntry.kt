package com.app.builder.domain.gateway.registry

import com.app.builder.domain.Registry

/**
 * A [Registry] paired with the name of the user that made the request.
 *
 * @property registry The registry.
 * @property userName The name of the user with [Registry.userUuid], or `null` when the user is not stored locally or the account cannot read users.
 */
data class RegistryEntry(
    val registry: Registry,
    val userName: String?,
)
