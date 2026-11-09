package com.app.builder.ui.navigation.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.UseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.button.PermissionItem
import com.app.builder.ui.component.filters.FiltersState
import com.app.builder.ui.component.filters.FiltersStore
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.component.session.SessionStore
import com.app.builder.ui.navigation.Router
import com.app.builder.ui.navigation.Screen

class UserDetailScreenTest: TestCase() {

    /**
     * Stores [FakeData.user] with its account [FakeData.account] as the admin, then logs in with [credentials].
     *
     * @param credentials The credentials to log in with once the user is stored.
     * @return The admin's [UseCases].
     */
    private suspend fun storedUserUseCases(credentials: UserCredentials = FakeData.adminCredentials): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        useCases.accountUseCases.createAccount(registrationForm = FakeData.registrationForm)
        if (credentials != FakeData.adminCredentials) useCases.authenticationUseCases.login(credentials = credentials)
        return useCases
    }

    /**
     * Renders the user detail screen of the user with [userUuid] with an action bar offering every user detail action, as the user entry provider does.
     *
     * @param useCases The use cases backing the stores.
     * @param userUuid The uuid of the user to display.
     * @param new Whether the user is new and not stored yet.
     * @return The [UserDetailScreenStore] and the [SessionStore] rendered.
     */
    private fun ComposeUiTest.setUserDetailScreen(useCases: UseCases, userUuid: String = FakeData.user.uuid.toString(), new: Boolean = false): Pair<UserDetailScreenStore, SessionStore> {
        val store = UserDetailScreenStore(
            state = UserDetailScreenState(),
            router = router,
            userUseCases = useCases.userUseCases,
            accountUseCases = useCases.accountUseCases,
            authenticationUseCases = useCases.authenticationUseCases,
            userUuid = userUuid,
            new = new
        )
        val sessionStore = SessionStore(state = SessionState(), authenticationUseCases = useCases.authenticationUseCases)

        setUI {
            UserDetailScreen(
                sessionStore = sessionStore,
                filtersStore = FiltersStore(state = FiltersState()),
                store = store
            )
        }

        return store to sessionStore
    }

    /** Verifies that a user with an active account is shown read only with its account, and can be edited but not deleted. */
    @Test
    fun viewUser() = runUITest {
        val (store, _) = setUserDetailScreen(useCases = storedUserUseCases())

        waitUntil { store.state.user != null && store.state.savedAccount != null && store.state.accountWritable }
        assertFalse(actual = store.state.editing)
        onNodeWithTag(testTag = "action_bar").assertIsDisplayed()
        onNodeWithTag(testTag = "navigation_bar").assertIsDisplayed()
        onNodeWithText(text = FakeData.user.name).assert(matcher = !hasSetTextAction())
        onNodeWithText(text = "username").assertIsDisplayed()
        onNode(matcher = SemanticsMatcher.keyIsDefined(key = SemanticsProperties.EditableText) and hasText(text = FakeData.account.username)).assertDoesNotExist()
        onNodeWithText(text = "password").assertDoesNotExist()
        onNodeWithTag(testTag = "account_active").assertIsOn().assertIsNotEnabled()
        onNodeWithTag(testTag = "${EntityType.TASK.name}_read").assertIsOn().assertIsNotEnabled()
        onNodeWithTag(testTag = "${EntityType.TASK.name}_write").assertIsOff()
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that a user with only read permission views the user read only, without any of the edit buttons. */
    @Test
    fun readOnlyUser() = runUITest {
        val (store, sessionStore) = setUserDetailScreen(useCases = storedUserUseCases(credentials = FakeData.credentials))

        waitUntil { store.state.user != null && store.state.accountReadable && sessionStore.state.user?.name == FakeData.user.name }
        assertFalse(actual = store.state.userWritable)
        assertFalse(actual = store.state.accountWritable)
        onNodeWithText(text = FakeData.user.name).assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that the viewed user follows the changes to the stored user, but not while it is being edited. */
    @Test
    fun viewFollowsUser() = runUITest {
        val useCases = storedUserUseCases()
        val (store, _) = setUserDetailScreen(useCases = useCases)

        waitUntil { store.state.user != null }
        useCases.userUseCases.upsertUser(user = FakeData.user.copy(name = "Remote name"))
        waitUntil { store.state.user?.name == "Remote name" }
        onNodeWithText(text = "Remote name").assertIsDisplayed()

        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        useCases.userUseCases.upsertUser(user = FakeData.user.copy(name = "Other name"))
        waitUntil { store.state.savedUser?.name == "Other name" }
        assertEquals(expected = "Remote name", actual = store.state.user?.name)
    }

    /** Verifies that saving persists the edited user and account, including a toggled permission, and goes back to viewing them. */
    @Test
    fun saveUser() = runUITest {
        val useCases = storedUserUseCases()
        val (store, _) = setUserDetailScreen(useCases = useCases)

        waitUntil { store.state.savedAccount != null && store.state.accountWritable }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.user.name).assertIsEnabled().performTextReplacement(text = "New name")
        onNode(matcher = SemanticsMatcher.keyIsDefined(key = SemanticsProperties.EditableText) and hasText(text = FakeData.account.username)).assertIsEnabled().performTextReplacement(text = "newuser")
        onNodeWithTag(testTag = "${EntityType.TASK.name}_write").assertIsEnabled()
        store.send(action = UserDetailScreenAction.TogglePermission(permission = PermissionItem(id = EntityType.TASK.name, read = true, write = true)))
        waitUntil { store.state.account?.permissions?.get(EntityType.TASK) == Permission.WRITE }
        store.send(action = UserDetailScreenAction.TogglePermission(permission = PermissionItem(id = EntityType.ACCOUNT.name, read = false, write = false)))
        waitUntil { store.state.account?.permissions?.containsKey(EntityType.ACCOUNT) == false }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        val savedUser = useCases.userUseCases.observeUser(uuid = FakeData.user.uuid).first { it?.name == "New name" }
        assertTrue(actual = savedUser!!.modifiedAt > FakeData.user.modifiedAt)
        val savedAccount = useCases.accountUseCases.observeAccount(userUuid = FakeData.user.uuid).first { it?.username == "newuser" }
        assertEquals(expected = Permission.WRITE, actual = savedAccount?.permissions?.get(EntityType.TASK))
        assertNull(actual = savedAccount?.permissions?.get(EntityType.ACCOUNT))
        assertEquals(expected = savedAccount, actual = store.state.savedAccount)
        onNodeWithText(text = "New name").assert(matcher = !hasSetTextAction())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
    }

    /** Verifies that disabling the account saves it disabled, which then allows deleting the user. */
    @Test
    fun disableAccount() = runUITest {
        val useCases = storedUserUseCases()
        val (store, _) = setUserDetailScreen(useCases = useCases)

        waitUntil { store.state.savedAccount != null && store.state.accountWritable }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithTag(testTag = "account_active").assertIsEnabled().performClick()
        waitUntil { store.state.account?.deletedAt != null }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        assertNotNull(actual = useCases.accountUseCases.observeAccount(userUuid = FakeData.user.uuid).first { it?.deletedAt != null })
        onNodeWithTag(testTag = "account_active").assertIsOff()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed()
    }

    /** Verifies that a failed account update keeps editing, with the save and cancel buttons, while the user edits are still saved. */
    @Test
    fun saveUserOffline() = runUITest {
        val useCases = storedUserUseCases()
        val (store, _) = setUserDetailScreen(useCases = useCases)

        waitUntil { store.state.savedAccount != null && store.state.accountWritable }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.user.name).performTextReplacement(text = "New name")
        onNode(matcher = SemanticsMatcher.keyIsDefined(key = SemanticsProperties.EditableText) and hasText(text = FakeData.account.username)).performTextReplacement(text = "newuser")
        ClientFlags.set { it.copy(http = false) }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { store.state.savedUser?.name == "New name" }

        assertTrue(actual = store.state.editing)
        assertEquals(expected = FakeData.account.username, actual = useCases.accountUseCases.observeAccount(userUuid = FakeData.user.uuid).firstOrNull()?.username)
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
    }

    /** Verifies that cancel discards the user and account edits and goes back to viewing the stored ones. */
    @Test
    fun cancelEdit() = runUITest {
        val useCases = storedUserUseCases()
        val (store, _) = setUserDetailScreen(useCases = useCases)

        waitUntil { store.state.savedAccount != null && store.state.accountWritable }
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }
        onNodeWithText(text = FakeData.user.name).performTextReplacement(text = "New name")
        onNode(matcher = SemanticsMatcher.keyIsDefined(key = SemanticsProperties.EditableText) and hasText(text = FakeData.account.username)).performTextReplacement(text = "newuser")
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { !store.state.editing }

        assertEquals(expected = FakeData.user.name, actual = store.state.user?.name)
        assertEquals(expected = FakeData.account.username, actual = store.state.account?.username)
        onNodeWithText(text = FakeData.user.name).assert(matcher = !hasSetTextAction())
        assertEquals(expected = FakeData.user.name, actual = useCases.userUseCases.observeUser(uuid = FakeData.user.uuid).first()?.name)
    }

    /** Verifies that a new user opens in edit mode with a draft account and a password field, and is not stored yet. */
    @Test
    fun newUser() = runUITest {
        val useCases = storedUserUseCases()
        val userUuid = uuid()
        val (store, _) = setUserDetailScreen(useCases = useCases, userUuid = userUuid.toString(), new = true)

        waitUntil { store.state.account != null }
        assertTrue(actual = store.state.editing)
        assertTrue(actual = store.state.new)
        assertEquals(expected = userUuid, actual = store.state.user?.uuid)
        assertEquals(expected = userUuid, actual = store.state.account?.userUuid)
        onNodeWithText(text = "password").assertIsDisplayed()
        onNodeWithTag(testTag = "account_active").assertDoesNotExist()
        onNodeWithTag(testTag = "action_save").assertIsDisplayed()
        onNodeWithTag(testTag = "action_cancel").assertIsDisplayed()
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        assertNull(actual = useCases.userUseCases.observeUser(uuid = userUuid).first())
    }

    /** Verifies that saving a new user without a username stores the user without creating an account. */
    @Test
    fun saveNewUserWithoutAccount() = runUITest {
        val useCases = storedUserUseCases()
        val userUuid = uuid()
        val (store, _) = setUserDetailScreen(useCases = useCases, userUuid = userUuid.toString(), new = true)

        waitUntil { store.state.account != null }
        store.send(action = UserDetailScreenAction.ChangeName(name = "New user"))
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        assertFalse(actual = store.state.new)
        assertNull(actual = store.state.account)
        assertEquals(expected = "New user", actual = useCases.userUseCases.observeUser(uuid = userUuid).first { it != null }?.name)
        assertNull(actual = useCases.accountUseCases.observeAccount(userUuid = userUuid).firstOrNull())
    }

    /** Verifies that saving a new user with a username and password creates its account and clears the password. */
    @Test
    fun saveNewUserWithAccount() = runUITest {
        val useCases = storedUserUseCases()
        val userUuid = uuid()
        val (store, _) = setUserDetailScreen(useCases = useCases, userUuid = userUuid.toString(), new = true)

        waitUntil { store.state.account != null }
        store.send(action = UserDetailScreenAction.ChangeName(name = "New user"))
        store.send(action = UserDetailScreenAction.ChangeUsername(username = "newuser"))
        store.send(action = UserDetailScreenAction.ChangePassword(password = "secret"))
        waitUntil { store.state.password == "secret" }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.editing }

        assertEquals(expected = "", actual = store.state.password)
        assertEquals(expected = "newuser", actual = store.state.savedAccount?.username)
        assertEquals(expected = "newuser", actual = useCases.accountUseCases.observeAccount(userUuid = userUuid).first { it != null }?.username)
        onNodeWithText(text = "password").assertDoesNotExist()
    }

    /** Verifies that failing to create the account of a new user keeps editing, while the user is no longer new. */
    @Test
    fun saveNewUserOffline() = runUITest {
        val useCases = storedUserUseCases()
        val userUuid = uuid()
        val (store, _) = setUserDetailScreen(useCases = useCases, userUuid = userUuid.toString(), new = true)

        waitUntil { store.state.account != null }
        store.send(action = UserDetailScreenAction.ChangeUsername(username = "newuser"))
        waitUntil { store.state.account?.username == "newuser" }
        ClientFlags.set { it.copy(http = false) }
        onNodeWithTag(testTag = "action_save").performClick()
        waitUntil { !store.state.new }

        assertTrue(actual = store.state.editing)
        assertNull(actual = store.state.savedAccount)
    }

    /** Verifies that canceling a new user leaves the screen without storing it. */
    @Test
    fun cancelNewUser() = runUITest {
        val useCases = storedUserUseCases()
        val userUuid = uuid()

        router.navigate(screen = Screen.UserList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.UserDetail(uuid = userUuid.toString(), new = true), option = Router.NavOption.ADD)

        val (store, _) = setUserDetailScreen(useCases = useCases, userUuid = userUuid.toString(), new = true)

        waitUntil { store.state.user != null }
        onNodeWithTag(testTag = "action_cancel").performClick()
        waitUntil { router.backStack.size == 1 }

        assertEquals(expected = listOf<Any>(Screen.UserList), actual = router.backStack.toList())
        assertNull(actual = useCases.userUseCases.observeUser(uuid = userUuid).first())
    }

    /** Verifies that a user without an account can be deleted, soft deleting it, bumping its modification time, and navigating back. */
    @Test
    fun deleteUser() = runUITest {
        val useCases = storedUserUseCases()
        val user = FakeData.user.copy(uuid = uuid())
        useCases.userUseCases.upsertUser(user = user)

        router.navigate(screen = Screen.UserList, option = Router.NavOption.ADD)
        router.navigate(screen = Screen.UserDetail(uuid = user.uuid.toString()), option = Router.NavOption.ADD)

        val (store, _) = setUserDetailScreen(useCases = useCases, userUuid = user.uuid.toString())

        waitUntil { store.state.user != null && store.state.accountReadable }
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitUntil { router.backStack.size == 1 }

        assertEquals(expected = listOf<Any>(Screen.UserList), actual = router.backStack.toList())
        val deleted = useCases.userUseCases.observeUser(uuid = user.uuid).first { it?.deletedAt != null }
        assertEquals(expected = deleted?.deletedAt, actual = deleted?.modifiedAt)
    }

    /** Verifies that a deleted user hides edit, and that its delete toggle restores it, keeps viewing it and shows edit again. */
    @Test
    fun restoreUser() = runUITest {
        val useCases = storedUserUseCases()
        val deletedUser = FakeData.user.copy(uuid = uuid(), deletedAt = now())
        useCases.userUseCases.upsertUser(user = deletedUser)
        val (store, _) = setUserDetailScreen(useCases = useCases, userUuid = deletedUser.uuid.toString())

        waitUntil { store.state.user?.deletedAt != null }
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertIsDisplayed().performClick()
        waitUntil { store.state.user?.deletedAt == null }

        val restored = useCases.userUseCases.observeUser(uuid = deletedUser.uuid).first { it?.deletedAt == null }
        assertTrue(actual = restored!!.modifiedAt > deletedUser.modifiedAt)
        assertTrue(actual = router.backStack.isEmpty())
        onNodeWithTag(testTag = "action_edit").assertIsDisplayed()
    }

    /** Verifies that editing a user without an account starts a draft account, when the current user can create accounts. */
    @Test
    fun editUserWithoutAccount() = runUITest {
        val useCases = storedUserUseCases()
        val user = FakeData.user.copy(uuid = uuid())
        useCases.userUseCases.upsertUser(user = user)
        val (store, _) = setUserDetailScreen(useCases = useCases, userUuid = user.uuid.toString())

        waitUntil { store.state.user != null && store.state.accountWritable }
        assertNull(actual = store.state.account)
        onNodeWithTag(testTag = "action_edit").performClick()
        waitUntil { store.state.editing }

        assertEquals(expected = user.uuid, actual = store.state.account?.userUuid)
        assertEquals(expected = "", actual = store.state.account?.username)
        onNodeWithText(text = "password").assertIsDisplayed()
    }

    /** Verifies that an invalid user uuid loads nothing. */
    @Test
    fun invalidUserUuid() = runUITest {
        val (store, sessionStore) = setUserDetailScreen(useCases = storedUserUseCases(), userUuid = "invalid")

        waitUntil { sessionStore.state.account != null }
        waitForIdle()
        assertEquals(expected = UserDetailScreenState(), actual = store.state)
        onNodeWithTag(testTag = "action_edit").assertDoesNotExist()
        onNodeWithTag(testTag = "action_delete").assertDoesNotExist()
    }

    /** Verifies that toggling a permission for an unknown entity type leaves the account unchanged. */
    @Test
    fun toggleUnknownPermission() = runUITest {
        val (store, _) = setUserDetailScreen(useCases = storedUserUseCases())

        waitUntil { store.state.account != null }
        val account = store.state.account
        store.send(action = UserDetailScreenAction.TogglePermission(permission = PermissionItem(id = "UNKNOWN", name = "unknown", read = true, write = true)))
        waitForIdle()
        assertEquals(expected = account, actual = store.state.account)
    }
}
