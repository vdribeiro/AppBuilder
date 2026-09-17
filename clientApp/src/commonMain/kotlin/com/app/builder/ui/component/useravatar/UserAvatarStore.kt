package com.app.builder.ui.component.useravatar

import kotlinx.coroutines.Job
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.resource.ImageResource
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.store.Store

/**
 * Store backing the [UserAvatar] component.
 *
 * @param state The initial user avatar state.
 * @property authenticationUseCases Use cases used to observe the current user.
 */
class UserAvatarStore(
    state: UserAvatarState,
    private val authenticationUseCases: AuthenticationUseCases
): Store<UserAvatarState, Unit>(initialState = state) {

    init {
        setup()
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
                    userName = user?.name,
                    userImage = userImage
                )
            }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    companion object {
        private const val TAG = "UserAvatarStore"
    }
}
