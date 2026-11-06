package com.app.builder.ui.screen.userdetail

import kotlinx.coroutines.Job
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.User
import com.app.builder.domain.gateway.account.AccountUseCases
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the User Detail Screen, observing the user and its account while viewing them, and applying, saving and discarding edits while editing them.
 *
 * @param state Initial User Detail Screen state.
 * @property router The router used for navigation.
 * @property userUseCases Use cases used to observe and save the user.
 * @property accountUseCases Use cases used to observe, create and update the user's account.
 * @property authenticationUseCases Use cases used to observe the current user's account permissions.
 * @property userUuid UUID of the user being displayed.
 * @property new Whether the user is new and not stored yet, starting it in edit mode.
 */
class UserDetailScreenStore(
    state: UserDetailScreenState,
    private val router: Router,
    private val userUseCases: UserUseCases,
    private val accountUseCases: AccountUseCases,
    private val authenticationUseCases: AuthenticationUseCases,
    private val userUuid: String,
    private val new: Boolean = false
): Store<UserDetailScreenState, UserDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] user in edit mode, then observes whether the current user can read accounts and edit users and accounts, and the user with [userUuid] and its account, ignoring their changes while they're being edited.
     *
     * @return The [Job] representing this execution.
     */
    private fun setup(): Job = launch(id = "setup") {
        Telemetry.info(tag = TAG, message = "Setup")

        val uuid = userUuid.toUuid() ?: run {
            Telemetry.info(tag = TAG, message = "Invalid user uuid")
            return@launch
        }

        if (new) {
            val user = User(uuid = uuid, modifiedAt = now(), deletedAt = null, name = "", avatar = null)
            updateState { it.copy(user = user, editing = true, new = true) }
        }

        authenticationUseCases.observeCurrentAccount().observe(id = "current_account") { account ->
            val userWritable = account?.hasPermission(entityType = EntityType.USER, permission = Permission.WRITE) ?: false
            val accountReadable = account?.hasPermission(entityType = EntityType.ACCOUNT, permission = Permission.READ) ?: false
            val accountWritable = account?.hasPermission(entityType = EntityType.ACCOUNT, permission = Permission.WRITE) ?: false
            updateState { it.copy(userWritable = userWritable, accountReadable = accountReadable, accountWritable = accountWritable).withDraftAccount() }
        }

        userUseCases.observeUser(uuid = uuid).observe(id = "user") { user ->
            updateState { if (it.editing) it.copy(savedUser = user) else it.copy(user = user, savedUser = user) }
        }

        accountUseCases.observeAccount(userUuid = uuid).observe(id = "account") { account ->
            updateState { if (it.editing) it.copy(savedAccount = account) else it.copy(account = account, savedAccount = account) }
        }

        Telemetry.info(tag = TAG, message = "Setup complete")
    }

    override fun reducer(state: UserDetailScreenState, action: UserDetailScreenAction) {
        super.reducer(state = state, action = action)
        when (action) {
            is UserDetailScreenAction.ChangeName -> updateState { it.copy(user = it.user?.copy(name = action.name)) }
            is UserDetailScreenAction.ChangeUsername -> updateState { it.copy(account = it.account?.copy(username = action.username)) }
            is UserDetailScreenAction.ChangePassword -> updateState { it.copy(password = action.password) }
            is UserDetailScreenAction.ChangeAccountActive -> updateState { it.copy(account = it.account?.copy(deletedAt = if (action.active) null else now())) }
            is UserDetailScreenAction.TogglePermission -> updateState { it.copy(account = it.account?.withPermission(permission = action.permission)) }
            UserDetailScreenAction.Edit -> updateState { it.copy(editing = true).withDraftAccount() }
            UserDetailScreenAction.Save -> save(state = state)
            UserDetailScreenAction.Cancel -> cancel(state = state)
            UserDetailScreenAction.Delete -> delete(state = state)
            UserDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Saves the edited user when it changed and the current user can edit users, then creates its account when a username was given for a user without one, or updates its changed account, and stops editing.
     * Creating and updating an account need a connection, so the screen stays in edit mode when either fails.
     *
     * @param state User Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: UserDetailScreenState): Job = launch(id = "save") {
        val editedUser = state.user ?: return@launch
        val user = when {
            state.userWritable && editedUser != state.savedUser -> editedUser.copy(modifiedAt = now()).also { userUseCases.upsertUser(user = it) }
            else -> state.savedUser ?: editedUser
        }

        val account = state.account
        val savedAccount = when {
            account == null || !state.accountWritable -> state.savedAccount
            state.savedAccount == null && account.username.isBlank() -> null
            state.savedAccount == null -> {
                val registrationForm = RegistrationForm(user = user, account = account.copy(modifiedAt = now()), password = state.password)
                accountUseCases.createAccount(registrationForm = registrationForm) ?: run {
                    updateState { it.copy(user = user, savedUser = user, new = false) }
                    return@launch
                }
            }

            account != state.savedAccount -> accountUseCases.updateAccount(account = account.copy(modifiedAt = now())) ?: run {
                updateState { it.copy(user = user, savedUser = user, new = false) }
                return@launch
            }

            else -> state.savedAccount
        }
        updateState { it.copy(user = user, savedUser = user, account = savedAccount, savedAccount = savedAccount, editing = false, new = false, password = "") }
    }

    /**
     * Discards the edits, then reloads the stored user and account and stops editing them, or leaves the screen when the user is new.
     *
     * @param state User Detail Screen state at the time the cancel was requested.
     * @return The [Job] representing this execution.
     */
    private fun cancel(state: UserDetailScreenState): Job = launch(id = "cancel") {
        if (state.new) {
            router.back()
            return@launch
        }
        updateState { it.copy(user = state.savedUser, account = state.savedAccount, editing = false, password = "") }
    }

    /**
     * Soft deletes the user by setting its deletion and modification times, then leaves the screen.
     *
     * @param state User Detail Screen state at the time the delete was requested.
     * @return The [Job] representing this execution.
     */
    private fun delete(state: UserDetailScreenState): Job = launch(id = "delete") {
        val now = now()
        val user = state.user?.copy(deletedAt = now, modifiedAt = now)
        user?.let { userUseCases.upsertUser(user = it) }
        router.back()
    }

    /**
     * Clears the deletion of the user, so it is active again, and keeps viewing it.
     *
     * @param state User Detail Screen state at the time the restore was requested.
     * @return The [Job] representing this execution.
     */
    private fun restore(state: UserDetailScreenState): Job = launch(id = "restore") {
        val user = state.user?.copy(deletedAt = null, modifiedAt = now()) ?: return@launch
        userUseCases.upsertUser(user = user)
        updateState { it.copy(user = user) }
    }

    /**
     * Starts a draft account to create while editing a user without one, when the current user can create accounts.
     *
     * @return This state with a draft account, or unchanged if it isn't editing, already has an account, or can't create one.
     */
    private fun UserDetailScreenState.withDraftAccount(): UserDetailScreenState {
        val user = user ?: return this
        if (!editing || account != null || savedAccount != null || !accountWritable) return this
        return copy(account = Account(userUuid = user.uuid, modifiedAt = now(), deletedAt = null, username = "", permissions = emptyMap()))
    }

    /**
     * Applies the toggled [permission] item to this account's permissions, removing the entity type when it has neither read nor write access.
     *
     * @param permission The toggled permission item, identified by its [EntityType] name.
     * @return This account with the updated permissions, or unchanged if the item doesn't match a known [EntityType].
     */
    private fun Account.withPermission(permission: PermissionItem): Account {
        val entityType = permission.id.toEnumOrNull<EntityType>() ?: return this
        val permissions = when {
            permission.write -> permissions + (entityType to Permission.WRITE)
            permission.read -> permissions + (entityType to Permission.READ)
            else -> permissions - entityType
        }
        return copy(permissions = permissions)
    }

    companion object {
        private const val TAG = "UserDetailScreenStore"
    }
}
