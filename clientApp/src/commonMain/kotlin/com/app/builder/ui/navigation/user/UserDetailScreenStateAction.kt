package com.app.builder.ui.navigation.user

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.domain.Account
import com.app.builder.domain.User
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.LinkPermissions

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
    /**
     * Sets the buildings linked to the edited user.
     *
     * @param buildings The buildings to link.
     */
    data class SetBuildings(val buildings: ImmutableList<Link>): UserDetailScreenAction
    /**
     * Unlinks a building from the edited user.
     *
     * @param buildingUuid The unique identifier of the building.
     */
    data class RemoveBuilding(val buildingUuid: Uuid): UserDetailScreenAction
    /**
     * Sets the equipments linked to the edited user.
     *
     * @param equipments The equipments to link.
     */
    data class SetEquipments(val equipments: ImmutableList<Link>): UserDetailScreenAction
    /**
     * Unlinks an equipment from the edited user.
     *
     * @param equipmentUuid The unique identifier of the equipment.
     */
    data class RemoveEquipment(val equipmentUuid: Uuid): UserDetailScreenAction
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
    /** Dismisses the message currently shown to the user. */
    data object DismissMessage: UserDetailScreenAction
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
 * @property buildings The buildings linked to the user, including the edits that are not saved yet.
 * @property savedBuildings The buildings linked to the user as stored.
 * @property equipments The equipments linked to the user, including the edits that are not saved yet.
 * @property savedEquipments The equipments linked to the user as stored.
 * @property buildingLinks What the current user can do with the links to the buildings of the user.
 * @property equipmentLinks What the current user can do with the links to the equipments of the user.
 * @property message Translation key of the message shown to the user, or null when there is nothing to report.
 * @property messageArgs Arguments the message is formatted with.
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
    val buildings: ImmutableList<Link> = persistentListOf(),
    val savedBuildings: ImmutableList<Link> = persistentListOf(),
    val equipments: ImmutableList<Link> = persistentListOf(),
    val savedEquipments: ImmutableList<Link> = persistentListOf(),
    val buildingLinks: LinkPermissions = LinkPermissions(),
    val equipmentLinks: LinkPermissions = LinkPermissions(),
    val message: String? = null,
    val messageArgs: ImmutableList<String> = persistentListOf(),
)
