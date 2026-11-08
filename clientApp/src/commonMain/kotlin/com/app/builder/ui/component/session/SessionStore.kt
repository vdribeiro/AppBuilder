package com.app.builder.ui.component.session

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.gateway.UseCases
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.status.StatusUseCases
import com.app.builder.ui.store.Store

/**
 * Store following the current session.
 *
 * @param state The initial [SessionState].
 * @property authenticationUseCases Use cases used to observe the current user and account.
 * @property statusUseCases Use cases used to observe the application status, or `null` to never report it.
 */
class SessionStore(
    state: SessionState,
    private val authenticationUseCases: AuthenticationUseCases,
    private val statusUseCases: StatusUseCases? = null,
): Store<SessionState, Unit>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the current user, their account and the application status.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        combine(
            flow = authenticationUseCases.observeCurrentUser(),
            flow2 = authenticationUseCases.observeCurrentAccount()
        ) { user, account -> user to account }.observe(id = "current_session") { (user, account) ->
            updateState { it.copy(loaded = true, user = user, account = account) }
        }

        statusUseCases?.observeSynced()?.observe(id = "synced") { synced ->
            updateState { it.copy(synced = synced) }
        }
        statusUseCases?.observeClockTrusted()?.observe(id = "clock_trusted") { clockTrusted ->
            updateState { it.copy(clockTrusted = clockTrusted) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    companion object {
        private const val TAG = "SessionStore"

        /** The key of the session store among the stores shared by the navigation host. */
        const val KEY = "session"

        /**
         * Creates a session store following the session of [useCases].
         *
         * @param useCases The use cases the store observes.
         * @return The session store.
         */
        fun get(useCases: UseCases): Store<SessionState, Unit> =
            SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases, statusUseCases = useCases.statusUseCases)
    }
}
