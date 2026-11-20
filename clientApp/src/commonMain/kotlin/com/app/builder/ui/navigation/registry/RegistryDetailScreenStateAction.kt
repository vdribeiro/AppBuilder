package com.app.builder.ui.navigation.registry

import com.app.builder.domain.Registry

/**
 * State of the Registry Detail Screen.
 *
 * @property registry Registry being viewed, or `null` while it's still loading.
 * @property userName Name of the user that made the request, or `null` when unknown.
 */
data class RegistryDetailScreenState(
    val registry: Registry? = null,
    val userName: String? = null,
)
