package com.app.builder.ui.navigation.userprofile

import kotlinx.coroutines.Job
import com.app.builder.core.locale.now
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the Edit Profile Screen, observing the current user and handling name updates and the profile save.
 *
 * @param state Initial Edit Profile Screen state.
 * @property router The router used for navigation.
 * @property authenticationUseCases Use cases used to observe the current user.
 * @property userUseCases Use cases used to save the current user.
 */
class EditProfileScreenStore(
    state: EditProfileScreenState,
    private val router: Router,
    private val authenticationUseCases: AuthenticationUseCases,
    private val userUseCases: UserUseCases
): Store<EditProfileScreenState, EditProfileScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: EditProfileScreenState, action: EditProfileScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is EditProfileScreenAction.UpdateName -> updateState { it.copy(name = action.name) }
            is EditProfileScreenAction.UpdateAvatar -> updateState { it.copy(avatar = action.avatar) }
            EditProfileScreenAction.Submit -> submit(state = state)
        }
    }

    /**
     * Observes the current user, filling the name and avatar URL inputs with its values the first time it is loaded.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        authenticationUseCases.observeCurrentUser().observe(id = "current_user") { user ->
            updateState {
                if (it.user == null) it.copy(user = user, name = user?.name.orEmpty(), avatar = user?.avatar.orEmpty()) else it.copy(user = user)
            }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Saves the current user with the edited name and avatar URL when the name is filled and either differs from the stored one, then leaves the screen.
     * A blank avatar URL removes the avatar.
     *
     * @param state Edit Profile Screen state at the time the submission was requested.
     * @return The [Job] representing this execution.
     */
    private fun submit(state: EditProfileScreenState): Job = launch(id = "submit", replace = false) {
        val user = state.user ?: return@launch
        if (state.name.isBlank()) return@launch
        Telemetry.info(tag = TAG, message = "Saving the profile of user ${user.uuid}")

        val avatar = state.avatar.trim().ifEmpty { null }
        if (state.name != user.name || avatar != user.avatar) userUseCases.upsertUser(user = user.copy(name = state.name, avatar = avatar, modifiedAt = now()))
        router.back()

        Telemetry.info(tag = TAG, message = "Profile saved")
    }

    companion object {
        private const val TAG = "EditProfileScreenStore"
    }
}
