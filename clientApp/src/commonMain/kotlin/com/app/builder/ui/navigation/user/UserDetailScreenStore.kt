package com.app.builder.ui.navigation.user

import kotlin.time.Instant
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.domain.Account
import com.app.builder.domain.BuildingUser
import com.app.builder.domain.EntityType
import com.app.builder.domain.EquipmentUser
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.User
import com.app.builder.domain.gateway.account.AccountUseCases
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.buildinguser.BuildingUserUseCases
import com.app.builder.domain.gateway.equipmentuser.EquipmentUserUseCases
import com.app.builder.domain.gateway.user.UserUseCases
import com.app.builder.toEnumOrNull
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.link.Link
import com.app.builder.ui.component.link.linkChanges
import com.app.builder.ui.component.link.linkPermissions
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.store.Store

/**
 * Store backing the User Detail Screen, observing the user, its account and its linked buildings and equipments while viewing them, and applying, saving and discarding edits while editing them.
 *
 * @param state Initial User Detail Screen state.
 * @property router The router used for navigation.
 * @property userUseCases Use cases used to observe and save the user.
 * @property accountUseCases Use cases used to observe, create and update the user's account.
 * @property buildingUserUseCases Use cases used to observe and save the building users of the user.
 * @property equipmentUserUseCases Use cases used to observe and save the equipment users of the user.
 * @property authenticationUseCases Use cases used to observe the current user's account permissions.
 * @property userUuid UUID of the user being displayed.
 * @property new Whether the user is new and not stored yet, starting it in edit mode.
 */
class UserDetailScreenStore(
    state: UserDetailScreenState,
    private val router: Router,
    private val userUseCases: UserUseCases,
    private val accountUseCases: AccountUseCases,
    private val buildingUserUseCases: BuildingUserUseCases,
    private val equipmentUserUseCases: EquipmentUserUseCases,
    private val authenticationUseCases: AuthenticationUseCases,
    private val userUuid: String,
    private val new: Boolean = false
): Store<UserDetailScreenState, UserDetailScreenAction>(initialState = state) {
    init {
        setup()
    }

    /**
     * Starts a [new] user in edit mode, then observes whether the current user can read accounts and edit users and accounts and what it can do with the user's links, and the user with [userUuid], its account and its linked buildings and equipments, ignoring their changes while they're being edited.
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
            val user = User(uuid = uuid)
            updateState { it.copy(user = user, editing = true, new = true) }
        }

        combine(
            flow = authenticationUseCases.observeCurrentAccount(),
            flow2 = ClientFlags.flow
        ) { account, flags -> account to flags }.observe(id = "current_account") { (account, flags) ->
            val userWritable = account?.hasPermission(entityType = EntityType.USER, permission = Permission.WRITE) ?: false
            val accountReadable = account?.hasPermission(entityType = EntityType.ACCOUNT, permission = Permission.READ) ?: false
            val accountWritable = account?.hasPermission(entityType = EntityType.ACCOUNT, permission = Permission.WRITE) ?: false
            val buildingLinks = account.linkPermissions(enabled = flags.buildingUsers, linked = EntityType.BUILDING, link = EntityType.BUILDING_USER)
            val equipmentLinks = account.linkPermissions(enabled = flags.equipmentUsers, linked = EntityType.EQUIPMENT, link = EntityType.EQUIPMENT_USER)
            updateState {
                it.copy(
                    userWritable = userWritable,
                    accountReadable = accountReadable,
                    accountWritable = accountWritable,
                    buildingLinks = buildingLinks,
                    equipmentLinks = equipmentLinks
                ).withDraftAccount()
            }
        }

        userUseCases.observeUser(uuid = uuid).observe(id = "user") { user ->
            updateState { if (it.editing) it.copy(savedUser = user) else it.copy(user = user, savedUser = user) }
        }

        accountUseCases.observeAccount(userUuid = uuid).observe(id = "account") { account ->
            updateState { if (it.editing) it.copy(savedAccount = account) else it.copy(account = account, savedAccount = account) }
        }

        buildingUserUseCases.observeBuildingsOfUser(userUuid = uuid).observe(id = "buildings") { buildings ->
            val saved = buildings.map { Link(uuid = it.uuid, name = it.name) }.toImmutableList()
            updateState { it.copy(savedBuildings = saved, buildings = if (it.editing) it.buildings else saved) }
        }

        equipmentUserUseCases.observeEquipmentsOfUser(userUuid = uuid).observe(id = "equipments") { equipments ->
            val saved = equipments.map { Link(uuid = it.uuid, name = it.name) }.toImmutableList()
            updateState { it.copy(savedEquipments = saved, equipments = if (it.editing) it.equipments else saved) }
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
            is UserDetailScreenAction.SetBuildings -> updateState { it.copy(buildings = action.buildings) }
            is UserDetailScreenAction.RemoveBuilding -> updateState { current ->
                current.copy(buildings = current.buildings.filter { it.uuid != action.buildingUuid }.toImmutableList())
            }

            is UserDetailScreenAction.SetEquipments -> updateState { it.copy(equipments = action.equipments) }
            is UserDetailScreenAction.RemoveEquipment -> updateState { current ->
                current.copy(equipments = current.equipments.filter { it.uuid != action.equipmentUuid }.toImmutableList())
            }

            UserDetailScreenAction.Edit -> updateState { it.copy(editing = true).withDraftAccount() }
            UserDetailScreenAction.Save -> save(state = state)
            UserDetailScreenAction.Cancel -> cancel(state = state)
            UserDetailScreenAction.Delete -> delete(state = state)
            UserDetailScreenAction.Restore -> restore(state = state)
        }
    }

    /**
     * Saves the edited user when it changed and the current user can edit users, then saves its building users and equipment users, then creates its account when a username was given for a user without one, or updates its changed account, and stops editing.
     * Creating and updating an account need a connection, so the screen stays in edit mode when either fails.
     *
     * @param state User Detail Screen state at the time the save was requested.
     * @return The [Job] representing this execution.
     */
    private fun save(state: UserDetailScreenState): Job = launch(id = "save") {
        val editedUser = state.user ?: return@launch
        val modifiedAt = now()
        val user = when {
            state.userWritable && editedUser != state.savedUser -> editedUser.copy(modifiedAt = modifiedAt).also { userUseCases.upsertUser(user = it) }
            else -> state.savedUser ?: editedUser
        }
        saveBuildings(state = state, modifiedAt = modifiedAt)
        saveEquipments(state = state, modifiedAt = modifiedAt)

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
     * Upserts a building user for each building in [UserDetailScreenState.buildings] and not in [UserDetailScreenState.savedBuildings], and deletes it for each building in the latter and not in the former.
     *
     * @param state User Detail Screen state at the time the save was requested.
     * @param modifiedAt The modification time of the building users.
     */
    private suspend fun saveBuildings(state: UserDetailScreenState, modifiedAt: Instant) {
        val user = state.user ?: return
        val changes = linkChanges(edited = state.buildings, stored = state.savedBuildings)
        changes.upserted.forEach { link ->
            buildingUserUseCases.upsertBuildingUser(
                buildingUser = BuildingUser(modifiedAt = modifiedAt, buildingUuid = link.uuid, userUuid = user.uuid)
            )
        }
        changes.removed.forEach { link ->
            buildingUserUseCases.upsertBuildingUser(
                buildingUser = BuildingUser(modifiedAt = modifiedAt, deletedAt = modifiedAt, buildingUuid = link.uuid, userUuid = user.uuid)
            )
        }
    }

    /**
     * Upserts an equipment user for each equipment in [UserDetailScreenState.equipments] and not in [UserDetailScreenState.savedEquipments], and deletes it for each equipment in the latter and not in the former.
     *
     * @param state User Detail Screen state at the time the save was requested.
     * @param modifiedAt The modification time of the equipment users.
     */
    private suspend fun saveEquipments(state: UserDetailScreenState, modifiedAt: Instant) {
        val user = state.user ?: return
        val changes = linkChanges(edited = state.equipments, stored = state.savedEquipments)
        changes.upserted.forEach { link ->
            equipmentUserUseCases.upsertEquipmentUser(
                equipmentUser = EquipmentUser(modifiedAt = modifiedAt, equipmentUuid = link.uuid, userUuid = user.uuid)
            )
        }
        changes.removed.forEach { link ->
            equipmentUserUseCases.upsertEquipmentUser(
                equipmentUser = EquipmentUser(modifiedAt = modifiedAt, deletedAt = modifiedAt, equipmentUuid = link.uuid, userUuid = user.uuid)
            )
        }
    }

    /**
     * Discards the edits, then reloads the stored user and account and its stored linked buildings and equipments and stops editing them, or leaves the screen when the user is new.
     *
     * @param state User Detail Screen state at the time the cancel was requested.
     * @return The [Job] representing this execution.
     */
    private fun cancel(state: UserDetailScreenState): Job = launch(id = "cancel") {
        if (state.new) {
            router.back()
            return@launch
        }
        updateState { it.copy(user = state.savedUser, account = state.savedAccount, editing = false, password = "", buildings = it.savedBuildings, equipments = it.savedEquipments) }
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
        return copy(account = Account(userUuid = user.uuid))
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
