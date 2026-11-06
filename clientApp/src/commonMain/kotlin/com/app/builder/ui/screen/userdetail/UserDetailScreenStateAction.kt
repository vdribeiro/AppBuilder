package com.app.builder.ui.screen.userdetail

import com.app.builder.domain.Account
import com.app.builder.domain.User
import com.app.builder.ui.component.button.PermissionItem

/** Actions supported by the User Detail Screen. */
sealed interface UserDetailScreenAction {
    /**
     * Updates the user's name.
     *
     * @param name The new name value.
     */
    data class ChangeName(val name: String): UserDetailScreenAction
    /**
     * Updates the account's username.
     *
     * @param username The new username value.
     */
    data class ChangeUsername(val username: String): UserDetailScreenAction
    /**
     * Updates the password a new account is created with.
     *
     * @param password The new password value.
     */
    data class ChangePassword(val password: String): UserDetailScreenAction
    /**
     * Activates or disables the account.
     *
     * @param active Whether the account is active.
     */
    data class ChangeAccountActive(val active: Boolean): UserDetailScreenAction
    /**
     * Updates the account permission granted to a single entity.
     *
     * @param permission Permission item with its updated read/write flags.
     */
    data class TogglePermission(val permission: PermissionItem): UserDetailScreenAction
    /** Start editing the current user. */
    data object Edit: UserDetailScreenAction
    /** Save the edited user and its account, creating the account when it has none, and stop editing it. */
    data object Save: UserDetailScreenAction
    /** Discard the edits, then reload the stored user and stop editing it, or leave the screen when the user is new. */
    data object Cancel: UserDetailScreenAction
    /** Delete the current user. */
    data object Delete: UserDetailScreenAction
    /** Restore the current deleted user. */
    data object Restore: UserDetailScreenAction
}

/**
 * State of the User Detail Screen.
 *
 * @property user User being viewed or edited, or `null` while it's still loading.
 * @property savedUser The stored user, or `null` while it's still loading or when the user is new.
 * @property account The user's account being viewed or edited, a draft while creating one, or `null` when it has none or accounts can't be read.
 * @property savedAccount The user's stored account, or `null` when it has none or accounts can't be read.
 * @property userWritable Whether the current user can create, edit and delete users.
 * @property accountReadable Whether the current user can read accounts, needed to know the user's account isn't active before deleting it.
 * @property accountWritable Whether the current user can create and edit accounts.
 * @property editing Whether the user is being edited, which stops it from following the stored user.
 * @property new Whether the user is new and not stored yet.
 * @property password Password a new account is created with.
 */
data class UserDetailScreenState(
    val user: User? = null,
    val savedUser: User? = null,
    val account: Account? = null,
    val savedAccount: Account? = null,
    val userWritable: Boolean = false,
    val accountReadable: Boolean = false,
    val accountWritable: Boolean = false,
    val editing: Boolean = false,
    val new: Boolean = false,
    val password: String = "",
)
