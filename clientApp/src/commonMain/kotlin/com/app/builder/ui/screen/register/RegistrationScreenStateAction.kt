package com.app.builder.ui.screen.register

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.button.PermissionItem

/** Actions that can be dispatched to a registration screen store. */
sealed interface RegistrationScreenAction {
    /**
     * Updates the name input field.
     *
     * @param name New value for the name field.
     */
    data class UpdateName(val name: String): RegistrationScreenAction
    /**
     * Updates the username input field.
     *
     * @param username New value for the username field.
     */
    data class UpdateUsername(val username: String): RegistrationScreenAction
    /**
     * Updates the password input field.
     *
     * @param password New value for the password field.
     */
    data class UpdatePassword(val password: String): RegistrationScreenAction
    /**
     * Updates the permission granted to a single entity.
     *
     * @param permission Permission item with its updated read/write flags.
     */
    data class TogglePermission(val permission: PermissionItem): RegistrationScreenAction
    /** Triggers the registration submission process. */
    data object Submit: RegistrationScreenAction
}

/**
 * State of the registration screen, holding its input values, granted permissions, and submission status.
 *
 * @property loading Whether a registration request is currently in flight.
 * @property name Name input field value.
 * @property username Username input field value.
 * @property password Password input field value.
 * @property permissions Permissions granted to the new account.
 */
data class RegistrationScreenState(
    val loading: Boolean = false,
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val permissions: ImmutableList<PermissionItem> = persistentListOf(),
)
