package com.app.builder.ui.screen.register

import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.Store
import com.app.builder.ui.component.button.PermissionItem

/**
 * Store backing the registration screen, handling field updates, permission toggling, and account registration.
 *
 * @param state Initial registration screen state.
 * @property authenticationUseCases Use cases used to register the new account.
 */
class RegistrationScreenStore(
    state: RegistrationScreenState,
    private val authenticationUseCases: AuthenticationUseCases
): Store<RegistrationScreenState, RegistrationScreenAction>(initialState = state) {
    init {
        setup()
    }

    override fun reducer(state: RegistrationScreenState, action: RegistrationScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is RegistrationScreenAction.UpdateName -> updateState { it.copy(name = action.name) }
            is RegistrationScreenAction.UpdateUsername -> updateState { it.copy(username = action.username) }
            is RegistrationScreenAction.UpdatePassword -> updateState { it.copy(password = action.password) }
            is RegistrationScreenAction.TogglePermission -> togglePermission(permission = action.permission)
            RegistrationScreenAction.Submit -> submitRegister(state = state)
        }
    }

    /** Initializes the state with a permission item for each [EntityType]. */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val permissions = EntityType.entries.map {
            PermissionItem(
                id = it.name,
                name = it.name.lowercase(),
                description = "" // TODO
            )
        }.toPersistentList()
        updateState { it.copy(permissions = permissions) }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    /**
     * Replaces the matching permission item in state with the given, toggled [permission].
     *
     * @param permission Updated permission item to store in place of the one with the same id.
     */
    private fun togglePermission(permission: PermissionItem): Job = launch(id = "togglePermission") {
        val permissions = state.permissions
            .map { if (it.id == permission.id) permission else it }
            .toPersistentList()
        updateState { it.copy(permissions = permissions) }
    }

    /**
     * Builds a registration screen from the current [state] and registers the new user and credentials.
     *
     * @param state Current registration screen state used to build the user and credentials.
     */
    private fun submitRegister(state: RegistrationScreenState): Job = launch(id = "submitRegister") {
        Telemetry.info(tag = TAG, message = "Attempting registration for ${state.username}")

        updateState { it.copy(loading = true) }
        val registrationForm = RegistrationForm(
            user = User(
                uuid = uuid(),
                modifiedAt = now(),
                deletedAt = null,
                permissions = state.permissions.toPermissions(),
                name = state.name,
                avatar = null
            ),
            credentials = UserCredentials(username = state.username, password = state.password)
        )
        authenticationUseCases.register(registrationForm = registrationForm)
        updateState { it.copy(loading = false) }

        Telemetry.info(tag = TAG, message = "Registration finished")
    }

    /**
     * Converts this list of permission items into a map of [EntityType] to [Permission], dropping items with neither read nor write access or with an id that doesn't match a known entity type.
     *
     * @return Map of entity type to the granted permission.
     */
    private fun List<PermissionItem>.toPermissions(): Map<EntityType, Permission> = mapNotNull { item ->
        val entityType = item.id.toEnumOrNull<EntityType>() ?: return@mapNotNull null
        val permission = when {
            item.write -> Permission.WRITE
            item.read -> Permission.READ
            else -> return@mapNotNull null
        }
        entityType to permission
    }.toMap()

    companion object {
        private const val TAG = "RegisterScreenStore"
    }
}
