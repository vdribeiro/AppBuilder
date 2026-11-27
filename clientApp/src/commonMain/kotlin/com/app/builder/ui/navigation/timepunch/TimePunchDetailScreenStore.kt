package com.app.builder.ui.navigation.timepunch

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.TimePunch
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.timepunch.TimePunchUseCases
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Time Punch Detail Screen, observing the time punch and the usernames while viewing it, and applying, saving and discarding edits while editing it.
 *
 * @param state Initial Time Punch Detail Screen state.
 * @property router The router used for navigation.
 * @property timePunchUseCases Use cases used to observe and save the time punch.
 * @property userUseCases Use cases used to observe the users that punched the clock.
 * @property authenticationUseCases Use cases used to observe the current user.
 * @property timePunchUuid UUID of the time punch being displayed.
 * @property new Whether the time punch is new and not stored yet, starting it in edit mode.
 */
class TimePunchDetailScreenStore(
    state: TimePunchDetailScreenState,
    private val router: Router,
    private val timePunchUseCases: TimePunchUseCases,
    private val userUseCases: UserUseCases,
    private val authenticationUseCases: AuthenticationUseCases,
    private val timePunchUuid: String,
    private val new: Boolean = false
): Store<TimePunchDetailScreenState, TimePunchDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] time punch of the current user in edit mode, then observes the user names and the time punch with [timePunchUuid], ignoring its changes while it's being edited.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = timePunchUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid time punch uuid")
            return@launch
        }

        val currentUserFlow = authenticationUseCases.observeCurrentUser().toStateFlow(initialValue = null)
        combine(
            flow = userUseCases.observeUsers().toStateFlow(initialValue = emptyList()),
            flow2 = currentUserFlow
        ) { users, currentUser ->
            (users + listOfNotNull(currentUser)).associate { it.uuid to it.name }.toImmutableMap()
        }.observe(id = "userNames") { userNames ->
            updateState { it.copy(userNames = userNames) }
        }

        if (new) {
            val userUuid = currentUserFlow.first { it != null }?.uuid ?: return@launch
            updateState { it.copy(timePunch = TimePunch(uuid = uuid, userUuid = userUuid), editing = true, new = true) }
        }

        timePunchUseCases.observeTimePunch(uuid = uuid).observe(id = "timePunch") { timePunch ->
            updateState { if (it.editing) it else it.copy(timePunch = timePunch) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: TimePunchDetailScreenState, action: TimePunchDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is TimePunchDetailScreenAction.ChangeTime -> updateState { it.copy(timePunch = it.timePunch?.copy(time = action.time)) }
            is TimePunchDetailScreenAction.ChangeUser -> updateState { it.copy(timePunch = it.timePunch?.copy(userUuid = action.userUuid)) }
            TimePunchDetailScreenAction.Edit -> updateState { it.copy(editing = true) }
            TimePunchDetailScreenAction.Save -> save(state = state)
            TimePunchDetailScreenAction.Cancel -> cancel(state = state)
            TimePunchDetailScreenAction.Delete -> delete(state = state)
            TimePunchDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Saves the edited time punch and stops editing it.
     *
     * @param state Time Punch Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: TimePunchDetailScreenState): Job = launch(id = "save") {
        val timePunch = state.timePunch?.copy(modifiedAt = now()) ?: return@launch
        timePunchUseCases.upsertTimePunch(timePunch = timePunch)
        updateState { it.copy(timePunch = timePunch, editing = false, new = false) }
    }

    /**
     * Discards the edits, then reloads the stored time punch and stops editing it, or leaves the screen when the time punch is new.
     *
     * @param state Time Punch Detail Screen state at the time the cancel was requested.
     * @return The [Job] representing this execution.
     */
    private fun cancel(state: TimePunchDetailScreenState): Job = launch(id = "cancel") {
        if (state.new) {
            router.back()
            return@launch
        }
        val timePunch = state.timePunch?.let { timePunchUseCases.observeTimePunch(uuid = it.uuid).firstOrNull() }
        updateState { it.copy(timePunch = timePunch, editing = false) }
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
        updateState { it.copy(timePunch = timePunch) }
    }

    companion object {
        private const val TAG = "TimePunchDetailScreenStore"
    }
}
