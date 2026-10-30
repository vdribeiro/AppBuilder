package com.app.builder.ui.screen.registrylist

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.RegistryItem

/** Actions that can be dispatched to a registry list store. */
sealed interface RegistryListScreenAction {
    /**
     * Selects a registry, navigating to its detail screen.
     *
     * @param registryUuid Request UUID of the registry that was selected.
     */
    data class SelectRegistry(val registryUuid: String): RegistryListScreenAction
}

/**
 * State of the registry list.
 *
 * @property registries Registries currently displayed.
 */
data class RegistryListScreenState(
    val registries: ImmutableList<RegistryItem> = persistentListOf(),
)
