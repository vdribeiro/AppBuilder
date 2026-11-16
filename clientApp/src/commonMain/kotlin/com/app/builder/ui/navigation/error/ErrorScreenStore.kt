package com.app.builder.ui.navigation.error

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import com.app.builder.core.telemetry.Console
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.ui.store.Store

/**
 * Store backing the error screen, following the console logs.
 *
 * @param state The initial [ErrorScreenState].
 */
class ErrorScreenStore(
    state: ErrorScreenState,
): Store<ErrorScreenState, Unit>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the console logs.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        Console.logs
            .map { lines -> lines.joinToString(separator = "\n").ifBlank { null } }
            .observe(id = "logs") { logs -> updateState { it.copy(logs = logs) } }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    companion object {
        private const val TAG = "ErrorScreenStore"
    }
}
