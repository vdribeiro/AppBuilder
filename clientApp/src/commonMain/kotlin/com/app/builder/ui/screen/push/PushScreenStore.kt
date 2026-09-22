package com.app.builder.ui.screen.push

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.PushPayload
import com.app.builder.domain.User
import com.app.builder.domain.gateway.push.PushUseCases
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.ui.component.button.PushPayloadType
import com.app.builder.ui.component.button.UserItem
import com.app.builder.ui.store.Store

/**
 * Store backing the push notification screen, loading the available users to target and sending the composed Push Payloads.
 *
 * @param state The initial [PushScreenState].
 * @property pushUseCases The use cases used to send push payloads.
 * @property userUseCases The use cases used to observe the list of users available as recipients.
 */
class PushScreenStore(
    state: PushScreenState,
    private val pushUseCases: PushUseCases,
    private val userUseCases: UserUseCases,
): Store<PushScreenState, PushScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: PushScreenState, action: PushScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is PushScreenAction.SelectType -> updateState { it.copy(type = action.type, sent = null) }
            is PushScreenAction.ToggleBroadcast -> updateState { it.copy(broadcast = action.value) }
            is PushScreenAction.ToggleUser -> toggleUser(state = state, action = action)
            is PushScreenAction.UpdateTitle -> updateState { it.copy(notificationTitle = action.value) }
            is PushScreenAction.UpdateDescription -> updateState { it.copy(notificationDescription = action.value) }
            PushScreenAction.Send -> send(state = state)
        }
    }

    /**
     * Observes the list of users and updates state with them mapped to [UserItem]s.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        userUseCases.observeUsers().observe(id = "users") { users ->
            val persistentUsers = users.map { it.toUserItem() }.toPersistentList()
            updateState { it.copy(users = persistentUsers) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Adds or removes [action]'s user from the current selection, depending on whether it was already selected.
     *
     * @param state The current state.
     * @param action The [PushScreenAction.ToggleUser] action.
     * @return The [Job] representing this execution.
     */
    private fun toggleUser(state: PushScreenState, action: PushScreenAction.ToggleUser): Job = launch(id = "toggleUser") {
        val filteredList = state.selectedUsers.filterNot { user -> user.uuid == action.user.uuid }
        val usersList = if (filteredList.size == state.selectedUsers.size) state.selectedUsers + action.user else filteredList
        val selectedUsers = usersList.toPersistentList()
        updateState { it.copy(selectedUsers = selectedUsers) }
    }

    /**
     * Builds the push payload(s) from [state] and sends them one by one, tracking how many succeeded.
     *
     * @param state The current state.
     * @return The [Job] representing this execution.
     */
    private fun send(state: PushScreenState): Job = launch(id = "send") {
        Telemetry.info(tag = TAG, message = "Send: type=${state.type}, broadcast=${state.broadcast}, selectedUsers=${state.selectedUsers}, title=${state.notificationTitle}, description=${state.notificationDescription}")

        val payloads = state.toPushPayloads()

        updateState { it.copy(sending = true, sent = null) }

        var count = 0
        payloads.forEach { if (pushUseCases.push(pushPayload = it)) count++ }
        updateState { it.copy(sending = false, sent = count) }

        Telemetry.info(tag = TAG, message = "Sent $count payloads")
    }

    /**
     * Converts this state into one [PushPayload] per targeted recipient:
     * a single broadcast payload when [broadcast] is set, or one payload per selected user otherwise.
     *
     * @return The list of payloads to send.
     */
    private fun PushScreenState.toPushPayloads(): List<PushPayload> =
        if (broadcast) listOfNotNull(element = toPushPayload(userUuid = null)) else {
            selectedUsers.mapNotNull { user -> toPushPayload(userUuid = user.uuid.toUuid()) }
        }

    /**
     * Maps this state's selected [type] to the matching [PushPayload] for the given [userUuid].
     *
     * @param userUuid The recipient's UUID, or `null` to target all users.
     * @return The built [PushPayload], or `null` if it could not be built.
     */
    private fun PushScreenState.toPushPayload(userUuid: Uuid?): PushPayload? = when (type) {
        PushPayloadType.NOTIFICATION -> PushPayload.Notification(userUuid = userUuid, title = notificationTitle, description = notificationDescription)
        PushPayloadType.FLAGS -> PushPayload.Flags(userUuid = userUuid)
        PushPayloadType.CONFIGS -> PushPayload.Configs(userUuid = userUuid)
        PushPayloadType.RESET -> PushPayload.Reset(userUuid = userUuid)
    }

    /**
     * Maps this domain [User] to a [UserItem] for display in the user selector.
     *
     * @return The mapped [UserItem].
     */
    private fun User.toUserItem(): UserItem = UserItem(
        uuid = uuid.toString(),
        name = name
    )

    companion object {
        private const val TAG = "PushScreenStore"
    }
}