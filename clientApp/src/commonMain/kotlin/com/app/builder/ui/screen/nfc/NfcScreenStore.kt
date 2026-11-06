package com.app.builder.ui.screen.nfc

import kotlinx.coroutines.Job
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.ui.store.Store

/**
 * Store backing the NFC screen, exposing whether the current user can write NFC tags.
 *
 * @param state The initial [NfcScreenState].
 * @property authenticationUseCases The use cases used to observe the current user's permissions.
 */
class NfcScreenStore(
    state: NfcScreenState,
    private val authenticationUseCases: AuthenticationUseCases,
): Store<NfcScreenState, Unit>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the current account and updates [NfcScreenState.writable] from its [EntityType.NFC] permission.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        authenticationUseCases.observeCurrentAccount().observe(id = "current_account") { account ->
            val writable = account?.hasPermission(entityType = EntityType.NFC, permission = Permission.WRITE) ?: false
            updateState { it.copy(writable = writable) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    companion object {
        private const val TAG = "NfcScreenStore"
    }
}
