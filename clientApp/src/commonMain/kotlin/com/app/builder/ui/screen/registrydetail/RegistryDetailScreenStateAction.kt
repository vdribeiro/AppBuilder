package com.app.builder.ui.screen.registrydetail

import com.app.builder.domain.Registry

/**
 * State of the Registry Detail Screen.
 *
 * @property registry Registry being viewed, or `null` while it's still loading.
 */
data class RegistryDetailScreenState(
    val registry: Registry? = null,
)
