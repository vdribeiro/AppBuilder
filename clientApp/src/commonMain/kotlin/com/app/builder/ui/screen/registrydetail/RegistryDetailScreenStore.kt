package com.app.builder.ui.screen.registrydetail

import kotlinx.coroutines.Job
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.gateway.registry.RegistryUseCases
import com.app.builder.ui.store.Store

/**
 * Store backing the Registry Detail Screen, observing the registry being viewed.
 *
 * @param state Initial Registry Detail Screen state.
 * @property registryUseCases Use cases used to observe the registry.
 * @property registryUuid Request UUID of the registry being displayed.
 */
class RegistryDetailScreenStore(
    state: RegistryDetailScreenState,
    private val registryUseCases: RegistryUseCases,
    private val registryUuid: String,
): Store<RegistryDetailScreenState, Unit>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the registry with [registryUuid].
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = registryUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid registry uuid")
            return@launch
        }

        registryUseCases.observeRegistry(uuid = uuid).observe(id = "registry") { registry ->
            updateState { it.copy(registry = registry) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    companion object {
        private const val TAG = "RegistryDetailScreenStore"
    }
}
