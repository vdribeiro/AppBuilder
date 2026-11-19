package com.app.builder.ui.navigation.userprofile

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Application
import com.app.builder.core.locale.SelectedLanguage
import com.app.builder.core.locale.observeLanguage
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.resource.ImageResource
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.translation.TranslationUseCases
import com.app.builder.ui.component.image.toImage
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.store.Store

/**
 * Store backing the user profile screen.
 *
 * @param state The initial [UserProfileScreenState].
 * @property authenticationUseCases Use cases used to observe and log out the current user.
 * @property translationUseCases Use cases used to observe the languages holding translations and to select the app language.
 */
class UserProfileScreenStore(
    state: UserProfileScreenState,
    private val authenticationUseCases: AuthenticationUseCases,
    private val translationUseCases: TranslationUseCases,
): Store<UserProfileScreenState, UserProfileScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: UserProfileScreenState, action: UserProfileScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            UserProfileScreenAction.Notifications -> {} // TODO
            UserProfileScreenAction.Logout -> logout()
            UserProfileScreenAction.Reset -> reset()
            UserProfileScreenAction.OpenLanguageDialog -> updateState { it.copy(languageDialogOpen = true, pickedLanguage = state.selectedLanguage) }
            is UserProfileScreenAction.PickLanguage -> updateState { it.copy(pickedLanguage = action.languageIso) }
            UserProfileScreenAction.ApplyLanguage -> applyLanguage(state = state)
            UserProfileScreenAction.DismissLanguageDialog -> updateState { it.copy(languageDialogOpen = false) }
        }
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
                    name = user?.name,
                    image = userImage
                )
            }
        }

        observeLanguage().observe(id = "language") { language ->
            updateState { it.copy(language = language) }
        }

        SelectedLanguage.flow.observe(id = "selected_language") { selectedLanguage ->
            updateState { it.copy(selectedLanguage = selectedLanguage) }
        }

        translationUseCases.observeLanguages().observe(id = "languages") { languages ->
            updateState { it.copy(languages = languages.toPersistentList()) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Logs out the current user.
     *
     * @return The [Job] representing this execution.
     */
    private fun logout(): Job = launch(id = "logout") {
        Telemetry.info(tag = TAG, message = "Logout")

        authenticationUseCases.observeCurrentUser().firstOrNull()?.let {
            authenticationUseCases.logout(userUuid = it.uuid)
        }
    }

    /**
     * Closes the language dialog and selects the language picked in it as the language the app is shown in.
     *
     * @param state State holding the picked language.
     * @return The [Job] representing this execution.
     */
    private fun applyLanguage(state: UserProfileScreenState): Job = launch(id = "applyLanguage") {
        Telemetry.info(tag = TAG, message = "Apply language ${state.pickedLanguage}")

        updateState { it.copy(languageDialogOpen = false) }
        translationUseCases.selectLanguage(languageIso = state.pickedLanguage)
    }

    /**
     * Resets the app's local data.
     *
     * @return The [Job] representing this execution.
     */
    private fun reset(): Job = launch(id = "reset") {
        Telemetry.info(tag = TAG, message = "Reset")

        Application.reset()
    }

    companion object {
        private const val TAG = "UserProfileStore"
    }
}