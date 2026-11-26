package com.app.builder.ui.navigation.timepunch

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.timepunch.TimePunchUseCases
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Time Punch Detail Screen, observing the time punch and its user, and deleting and restoring it.
 *
 * @param state Initial Time Punch Detail Screen state.
 * @property router The router used for navigation.
 * @property timePunchUseCases Use cases used to observe and save the time punch.
 * @property userUseCases Use cases used to observe the users that punched the clock.
 * @property authenticationUseCases Use cases used to observe the current user.
 * @property timePunchUuid UUID of the time punch being displayed.
 */
class TimePunchDetailScreenStore(
    state: TimePunchDetailScreenState,
    private val router: Router,
    private val timePunchUseCases: TimePunchUseCases,
    private val userUseCases: UserUseCases,
    private val authenticationUseCases: AuthenticationUseCases,
    private val timePunchUuid: String
): Store<TimePunchDetailScreenState, TimePunchDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the time punch with [timePunchUuid] and the name of its user.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = timePunchUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid time punch uuid")
            return@launch
        }

        combine(
            flow = timePunchUseCases.observeTimePunch(uuid = uuid),
            flow2 = userUseCases.observeUsers().toStateFlow(initialValue = emptyList()),
            flow3 = authenticationUseCases.observeCurrentUser().toStateFlow(initialValue = null)
        ) { timePunch, users, currentUser ->
            timePunch to (users + listOfNotNull(currentUser)).firstOrNull { it.uuid == timePunch?.userUuid }?.name
        }.observe(id = "timePunch") { (timePunch, userName) ->
            updateState { it.copy(timePunch = timePunch, userName = userName) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: TimePunchDetailScreenState, action: TimePunchDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            TimePunchDetailScreenAction.Delete -> delete(state = state)
            TimePunchDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Soft deletes the time punch by setting its deletion and modification times, then leaves the screen.
     *
     * @param state Time Punch Detail Screen state at the time the delete was requested.
     * @return The [Job] representing this execution.
     */
    private fun delete(state: TimePunchDetailScreenState): Job = launch(id = "delete") {
        val now = now()
        val timePunch = state.timePunch?.copy(deletedAt = now, modifiedAt = now)
        timePunch?.let { timePunchUseCases.upsertTimePunch(timePunch = it) }
        router.back()
    }

    /**
     * Clears the deletion of the time punch, so it is active again, and keeps viewing it.
     *
     * @param state Time Punch Detail Screen state at the time the restore was requested.
     * @return The [Job] representing this execution.
     */
    private fun restore(state: TimePunchDetailScreenState): Job = launch(id = "restore") {
        val timePunch = state.timePunch?.copy(deletedAt = null, modifiedAt = now()) ?: return@launch
        timePunchUseCases.upsertTimePunch(timePunch = timePunch)
    }

    companion object {
        private const val TAG = "TimePunchDetailScreenStore"
    }
}
