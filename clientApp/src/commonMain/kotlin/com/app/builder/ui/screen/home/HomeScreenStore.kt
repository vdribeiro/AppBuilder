package com.app.builder.ui.screen.home

import kotlinx.coroutines.Job
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.resource.ImageResource
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.store.Store

/**
 * Store backing the home screen, observing the current user so the screen knows whether the session is authenticated.
 *
 * @param state The initial [HomeScreenState].
 * @property authenticationUseCases Use cases used to observe the current user.
 */
class HomeScreenStore(
    state: HomeScreenState,
    private val authenticationUseCases: AuthenticationUseCases,
): Store<HomeScreenState, Unit>(initialState = state) {
    init {
        setup()
    }

    /**
     * Observes the current user.
     *
     * @return The [Job] representing this execution.
     */
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
                    image = userImage
                )
            }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    companion object {
        private const val TAG = "HomeScreenStore"
    }
}
