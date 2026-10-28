package com.app.builder.ui.component.actionbar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.EntityType
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.gateway.authentication.AuthenticationUseCases
import com.app.builder.domain.gateway.status.StatusUseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.navigation.Screen

class ActionBarStoreTest: TestCase() {

    /** Action bar data the task list seeds its preferences with. */
    private val defaults = AppFile.ActionBarData(
        search = "",
        searchableProperties = listOf("TITLE", "DESCRIPTION"),
        sortAscending = false,
        sortProperty = "MODIFIED_AT",
        visibleProperties = listOf("TITLE", "DESCRIPTION", "STATE"),
    )

    /**
     * Resolves the authentication use cases of the guest graph, the ones that observe whoever is logged in.
     *
     * @return The [AuthenticationUseCases] instance.
     */
    private suspend fun authenticationUseCases(): AuthenticationUseCases = dependency.get()
        .userGraph
        .useCases
        .authenticationUseCases

    /**
     * Keeps the store's state observed, as a rendered action bar would, so it collects the current user.
     *
     * @param store The store to observe.
     * @return The collecting [Job].
     */
    private fun TestScope.subscribe(store: ActionBarStore): Job = backgroundScope.launch { store.stateFlow.collect {} }

    /**
     * Logs in, builds a store wired with every callback on the task entity, and waits until it reflects the logged in user.
     *
     * @param credentials The credentials to log in with.
     * @return The [ActionBarState] once the user is resolved.
     */
    private suspend fun TestScope.loggedInState(credentials: UserCredentials): ActionBarState {
        val authenticationUseCases = authenticationUseCases()
        authenticationUseCases.login(credentials = credentials)

        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases,
            entityType = EntityType.TASK,
            onAddClick = {},
            onEditClick = {},
            onSaveClick = {},
            onCancelClick = {},
            onDeleteClick = {},
        )
        subscribe(store = store)
        return store.stateFlow.first { it.avatarName != null }
    }

    /** Verifies that a logged out user is offered the login button and none of the entity buttons, even with every callback set. */
    @Test
    fun loggedOut() = runUnitTest {
        val store = ActionBarStore(
            state = ActionBarState(showLogin = false),
            router = router,
            authenticationUseCases = authenticationUseCases(),
            entityType = EntityType.TASK,
            onAddClick = {},
            onEditClick = {},
            onSaveClick = {},
            onCancelClick = {},
            onDeleteClick = {},
        )
        subscribe(store = store)

        val state = store.stateFlow.first { it.showLogin }
        assertFalse(actual = state.showAdd)
        assertFalse(actual = state.showEdit)
        assertFalse(actual = state.showSave)
        assertFalse(actual = state.showCancel)
        assertFalse(actual = state.showDelete)
        assertNull(actual = state.avatarName)
    }

    /** Verifies that a user with write permission on the entity views it with add, edit and delete, and no login button. */
    @Test
    fun writePermission() = runUnitTest {
        val state = loggedInState(credentials = FakeData.adminCredentials)

        assertEquals(expected = FakeData.adminUser.name, actual = state.avatarName)
        assertTrue(actual = state.writable)
        assertFalse(actual = state.editing)
        assertTrue(actual = state.showAdd)
        assertTrue(actual = state.showEdit)
        assertFalse(actual = state.showSave)
        assertFalse(actual = state.showCancel)
        assertTrue(actual = state.showDelete)
        assertFalse(actual = state.showLogin)
    }

    /** Verifies that a user with only read permission on the entity is offered none of the entity buttons. */
    @Test
    fun readPermission() = runUnitTest {
        val state = loggedInState(credentials = FakeData.credentials)

        assertEquals(expected = FakeData.user.name, actual = state.avatarName)
        assertFalse(actual = state.writable)
        assertFalse(actual = state.showAdd)
        assertFalse(actual = state.showEdit)
        assertFalse(actual = state.showSave)
        assertFalse(actual = state.showCancel)
        assertFalse(actual = state.showDelete)
    }

    /** Verifies that the entity buttons stay hidden when their callbacks are missing, or when the store has no entity to check permissions against. */
    @Test
    fun missingCallbacksOrEntity() = runUnitTest {
        val authenticationUseCases = authenticationUseCases()
        authenticationUseCases.login(credentials = FakeData.adminCredentials)

        val withoutCallbacks = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases,
            entityType = EntityType.TASK,
        )
        subscribe(store = withoutCallbacks)
        val withoutCallbacksState = withoutCallbacks.stateFlow.first { !it.showLogin }
        assertFalse(actual = withoutCallbacksState.showAdd)
        assertFalse(actual = withoutCallbacksState.showEdit)
        assertFalse(actual = withoutCallbacksState.showSave)
        assertFalse(actual = withoutCallbacksState.showDelete)

        val withoutEntity = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases,
            onAddClick = {},
            onEditClick = {},
            onSaveClick = {},
            onCancelClick = {},
            onDeleteClick = {},
        )
        subscribe(store = withoutEntity)
        val withoutEntityState = withoutEntity.stateFlow.first { !it.showLogin }
        assertFalse(actual = withoutEntityState.showAdd)
        assertFalse(actual = withoutEntityState.showEdit)
        assertFalse(actual = withoutEntityState.showSave)
        assertFalse(actual = withoutEntityState.showDelete)
    }

    /** Verifies that the add and delete actions invoke their callbacks. */
    @Test
    fun addAndDelete() = runUnitTest {
        var added = 0
        var deleted = 0
        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases(),
            onAddClick = { added++ },
            onDeleteClick = { deleted++ },
        )

        store.send(action = ActionBarAction.Add)
        testScheduler.advanceUntilIdle()
        assertEquals(expected = 1, actual = added)
        assertEquals(expected = 0, actual = deleted)

        store.send(action = ActionBarAction.Delete)
        testScheduler.advanceUntilIdle()
        assertEquals(expected = 1, actual = added)
        assertEquals(expected = 1, actual = deleted)
    }

    /** Verifies that edit switches to save and cancel, and that save and cancel switch back to add, edit and delete, invoking their callbacks. */
    @Test
    fun editSaveAndCancel() = runUnitTest {
        val authenticationUseCases = authenticationUseCases()
        authenticationUseCases.login(credentials = FakeData.adminCredentials)
        var edited = 0
        var saved = 0
        var canceled = 0
        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases,
            entityType = EntityType.TASK,
            onAddClick = {},
            onEditClick = { edited++ },
            onSaveClick = { saved++ },
            onCancelClick = { canceled++ },
            onDeleteClick = {},
        )
        subscribe(store = store)
        store.stateFlow.first { it.showEdit }

        store.send(action = ActionBarAction.Edit)
        val editing = store.stateFlow.first { it.editing }
        assertEquals(expected = 1, actual = edited)
        assertFalse(actual = editing.showAdd)
        assertFalse(actual = editing.showEdit)
        assertTrue(actual = editing.showSave)
        assertTrue(actual = editing.showCancel)
        assertFalse(actual = editing.showDelete)

        store.send(action = ActionBarAction.Save)
        val saving = store.stateFlow.first { !it.editing }
        assertEquals(expected = 1, actual = saved)
        assertTrue(actual = saving.showAdd)
        assertTrue(actual = saving.showEdit)
        assertFalse(actual = saving.showSave)
        assertFalse(actual = saving.showCancel)
        assertTrue(actual = saving.showDelete)

        store.send(action = ActionBarAction.Edit)
        store.stateFlow.first { it.editing }
        store.send(action = ActionBarAction.Cancel)
        val canceling = store.stateFlow.first { !it.editing }
        testScheduler.advanceUntilIdle()
        assertEquals(expected = 2, actual = edited)
        assertEquals(expected = 1, actual = canceled)
        assertTrue(actual = canceling.showEdit)
        assertFalse(actual = canceling.showCancel)
    }

    /** Verifies that losing the write permission while editing switches back to viewing and invokes the cancel callback. */
    @Test
    fun permissionLostWhileEditing() = runUnitTest {
        val authenticationUseCases = authenticationUseCases()
        authenticationUseCases.login(credentials = FakeData.adminCredentials)
        var canceled = 0
        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases,
            entityType = EntityType.TASK,
            onEditClick = {},
            onSaveClick = {},
            onCancelClick = { canceled++ },
        )
        subscribe(store = store)
        store.stateFlow.first { it.showEdit }
        store.send(action = ActionBarAction.Edit)
        store.stateFlow.first { it.editing }

        authenticationUseCases.login(credentials = FakeData.credentials)
        val state = store.stateFlow.first { it.avatarName == FakeData.user.name }
        testScheduler.advanceUntilIdle()

        assertFalse(actual = state.editing)
        assertFalse(actual = state.writable)
        assertFalse(actual = state.showSave)
        assertFalse(actual = state.showCancel)
        assertEquals(expected = 1, actual = canceled)
    }

    /** Verifies that the status flags follow the status use cases, and stay at their defaults without them. */
    @Test
    fun status() = runUnitTest {
        val synced = MutableStateFlow(value = true)
        val clockTrusted = MutableStateFlow(value = true)
        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases(),
            statusUseCases = object: StatusUseCases {
                override fun observeSynced(): Flow<Boolean> = synced
                override fun observeClockTrusted(): Flow<Boolean> = clockTrusted
            },
        )
        subscribe(store = store)

        store.stateFlow.first { it.synced == true && it.clockTrusted }

        synced.value = false
        store.stateFlow.first { it.synced == false }

        clockTrusted.value = false
        store.stateFlow.first { !it.clockTrusted }

        synced.value = true
        clockTrusted.value = true
        store.stateFlow.first { it.synced == true && it.clockTrusted }

        val withoutStatus = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticationUseCases())
        subscribe(store = withoutStatus)
        val withoutStatusState = withoutStatus.stateFlow.first { it.showLogin }
        assertNull(actual = withoutStatusState.synced)
        assertTrue(actual = withoutStatusState.clockTrusted)
    }

    /** Verifies that the avatar opens the user profile and the login button opens the login screen. */
    @Test
    fun navigation() = runUnitTest {
        val store = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticationUseCases())

        store.send(action = ActionBarAction.OpenUser)
        assertEquals(expected = listOf(Screen.UserProfile), actual = router.backStack.toList())

        store.send(action = ActionBarAction.Login)
        assertEquals(expected = listOf(Screen.UserProfile, Screen.Login), actual = router.backStack.toList())
    }

    /** Verifies that with nothing persisted yet, the defaults are both applied to the state and written to the storage file. */
    @Test
    fun seedDefaults() = runUnitTest {
        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases(),
            storageFile = AppFile.TaskPreferences,
            defaults = defaults,
        )
        testScheduler.advanceUntilIdle()

        assertEquals(expected = defaults, actual = AppFile.TaskPreferences.load())
        assertEquals(expected = defaults.searchableProperties, actual = store.state.searchableProperties)
        assertEquals(expected = defaults.sortAscending, actual = store.state.sortAscending)
        assertEquals(expected = defaults.sortProperty, actual = store.state.sortProperty)
        assertEquals(expected = defaults.visibleProperties, actual = store.state.visibleProperties)
    }

    /** Verifies that a partially persisted payload keeps its own values and has only its missing ones backfilled with the defaults. */
    @Test
    fun backfillDefaults() = runUnitTest {
        AppFile.TaskPreferences.save { AppFile.ActionBarData(search = "query", sortAscending = true) }

        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases(),
            storageFile = AppFile.TaskPreferences,
            defaults = defaults,
        )
        testScheduler.advanceUntilIdle()

        val expected = defaults.copy(search = "query", sortAscending = true)
        assertEquals(expected = expected, actual = AppFile.TaskPreferences.load())
        assertTrue(actual = store.state.sortAscending)
        assertEquals(expected = defaults.sortProperty, actual = store.state.sortProperty)
        assertEquals(expected = defaults.searchableProperties, actual = store.state.searchableProperties)
        assertEquals(expected = defaults.visibleProperties, actual = store.state.visibleProperties)
    }

    /** Verifies that a fully persisted payload is applied as is, without being overwritten by the defaults. */
    @Test
    fun keepPersisted() = runUnitTest {
        val persisted = AppFile.ActionBarData(
            search = "query",
            searchableProperties = listOf("STATE"),
            sortAscending = true,
            sortProperty = "TITLE",
            visibleProperties = listOf("TITLE"),
        )
        AppFile.TaskPreferences.save { persisted }

        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases(),
            storageFile = AppFile.TaskPreferences,
            defaults = defaults,
        )
        testScheduler.advanceUntilIdle()

        assertEquals(expected = persisted, actual = AppFile.TaskPreferences.load())
        assertEquals(expected = persisted.searchableProperties, actual = store.state.searchableProperties)
        assertEquals(expected = persisted.sortProperty, actual = store.state.sortProperty)
        assertEquals(expected = persisted.visibleProperties, actual = store.state.visibleProperties)
    }

    /** Verifies that searching, sorting and toggling properties update the state and persist every change to the storage file. */
    @Test
    fun persistChanges() = runUnitTest {
        val store = ActionBarStore(
            state = ActionBarState(),
            router = router,
            authenticationUseCases = authenticationUseCases(),
            storageFile = AppFile.TaskPreferences,
            defaults = defaults,
        )
        testScheduler.advanceUntilIdle()

        store.send(action = ActionBarAction.Search(search = "query"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = "query", actual = AppFile.TaskPreferences.load()?.search)

        store.send(action = ActionBarAction.SelectSortOrder(ascending = true))
        testScheduler.advanceUntilIdle()
        assertTrue(actual = store.state.sortAscending)
        assertEquals(expected = true, actual = AppFile.TaskPreferences.load()?.sortAscending)

        store.send(action = ActionBarAction.SelectSortProperty(property = "TITLE"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = "TITLE", actual = store.state.sortProperty)
        assertEquals(expected = "TITLE", actual = AppFile.TaskPreferences.load()?.sortProperty)

        store.send(action = ActionBarAction.ToggleSearchableProperty(property = "TITLE"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = listOf("DESCRIPTION"), actual = store.state.searchableProperties)
        assertEquals(expected = listOf("DESCRIPTION"), actual = AppFile.TaskPreferences.load()?.searchableProperties)

        store.send(action = ActionBarAction.ToggleSearchableProperty(property = "TITLE"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = listOf("DESCRIPTION", "TITLE"), actual = store.state.searchableProperties)
        assertEquals(expected = listOf("DESCRIPTION", "TITLE"), actual = AppFile.TaskPreferences.load()?.searchableProperties)

        store.send(action = ActionBarAction.ToggleVisibleProperty(property = "STATE"))
        testScheduler.advanceUntilIdle()
        assertEquals(expected = listOf("TITLE", "DESCRIPTION"), actual = store.state.visibleProperties)
        assertEquals(expected = listOf("TITLE", "DESCRIPTION"), actual = AppFile.TaskPreferences.load()?.visibleProperties)

        val persisted = AppFile.TaskPreferences.load()
        assertEquals(expected = "query", actual = persisted?.search)
        assertEquals(expected = true, actual = persisted?.sortAscending)
        assertEquals(expected = "TITLE", actual = persisted?.sortProperty)
    }

    /** Verifies that without a storage file the sorting and property filters still apply to the state. */
    @Test
    fun inMemory() = runUnitTest {
        val store = ActionBarStore(
            state = ActionBarState(visibleProperties = persistentListOf("TITLE")),
            router = router,
            authenticationUseCases = authenticationUseCases(),
        )

        store.send(action = ActionBarAction.SelectSortOrder(ascending = false))
        store.send(action = ActionBarAction.SelectSortProperty(property = "STATE"))
        store.send(action = ActionBarAction.ToggleVisibleProperty(property = "STATE"))
        testScheduler.advanceUntilIdle()

        assertFalse(actual = store.state.sortAscending)
        assertEquals(expected = "STATE", actual = store.state.sortProperty)
        assertEquals(expected = listOf("TITLE", "STATE"), actual = store.state.visibleProperties)
        assertNull(actual = AppFile.TaskPreferences.load())
    }
}
