package com.app.builder.ui.screen.userprofile

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Application
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.resource.ImageResource
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.ui.component.Store
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.core.image.Image

/**
 * Store backing the user profile screen, observing the current user and handling logout and local data reset.
 *
 * @param state The initial [UserProfileScreenState].
 * @property authenticationUseCases Use cases used to observe and log out the current user.
 */
class UserProfileScreenStore(
    state: UserProfileScreenState,
    private val authenticationUseCases: AuthenticationUseCases,
): Store<UserProfileScreenState, UserProfileScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: UserProfileScreenState, action: UserProfileScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            UserProfileScreenAction.EditProfile -> {} // TODO
            UserProfileScreenAction.ChangePassword -> {}  // TODO
            UserProfileScreenAction.Notifications -> {} // TODO
            UserProfileScreenAction.Logout -> logout()
            UserProfileScreenAction.Reset -> reset()
        }
    }

    /** Observes the current user. */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        authenticationUseCases.observeCurrentUser().observe(id = "current_user") { user ->
            val userImage = when (user) {
                null -> ImageResource.Kotlin.toImage()
                else -> user.avatar?.let { avatar -> Image(url = avatar) }
            }
            updateState {
                it.copy(
                    guest = user == null,
                    name = user?.name,
                    image = userImage
                )
            }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /** Logs out the current user. */
    private fun logout(): Job = launch(id = "logout") {
        Telemetry.info(tag = TAG, message = "Logout")

        authenticationUseCases.observeCurrentUser().firstOrNull()?.let {
            authenticationUseCases.logout(userUuid = it.uuid)
        }
    }

    /** Resets the app's local data. */
    private fun reset(): Job = launch(id = "reset") {
        Telemetry.info(tag = TAG, message = "Reset")

        Application.reset()
    }

    companion object {
        private const val TAG = "UserProfileStore"
    }
}