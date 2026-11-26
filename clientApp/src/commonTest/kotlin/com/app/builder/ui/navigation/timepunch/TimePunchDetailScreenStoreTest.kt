package com.app.builder.ui.navigation.timepunch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.security.uuid
import com.app.builder.domain.TimePunch
import com.app.builder.domain.gateway.UseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TimePunchDetailScreenStoreTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, scheduler = true) }
    }

    /** A stored time punch of the admin user. */
    private val stored = TimePunch(
        userUuid = FakeData.adminUser.uuid,
        time = Instant.parse(input = "2020-01-01T09:00:00Z"),
    )

    /**
     * Logs the admin user in.
     *
     * @return The use cases of the admin user.
     */
    private suspend fun loggedInUseCases(): UseCases {
        val useCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        return useCases
    }

    /**
     * Creates the store of the given [useCases].
     *
     * @param useCases The use cases the store reads and writes with.
     * @param timePunchUuid UUID of the time punch the store shows.
     * @param new Whether the time punch is new.
     * @return The time punch detail store.
     */
    private fun store(useCases: UseCases, timePunchUuid: String, new: Boolean = false): TimePunchDetailScreenStore = TimePunchDetailScreenStore(
        state = TimePunchDetailScreenState(),
        router = router,
        timePunchUseCases = useCases.timePunchUseCases,
        userUseCases = useCases.userUseCases,
        authenticationUseCases = useCases.authenticationUseCases,
        timePunchUuid = timePunchUuid,
        new = new
    )

    /** Verifies that the store shows the stored time punch and the name of its user. */
    @Test
    fun showsTheStoredTimePunch() = runUnitTest {
        val useCases = loggedInUseCases()
        useCases.timePunchUseCases.upsertTimePunch(timePunch = stored)

        val state = store(useCases = useCases, timePunchUuid = stored.uuid.toString()).stateFlow.first { it.timePunch != null && it.userNames.isNotEmpty() }

        assertEquals(expected = stored.uuid, actual = state.timePunch?.uuid)
        assertEquals(expected = FakeData.adminUser.name, actual = state.userNames[stored.userUuid])
        assertFalse(actual = state.editing)
    }

    /** Verifies that a new time punch starts in edit mode for the current user, and is stored for the picked user at the changed time once saved. */
    @Test
    fun savesANewTimePunchForAnotherUser() = runUnitTest {
        val useCases = loggedInUseCases()
        val uuid = uuid()
        val time = Instant.parse(input = "2021-06-01T18:00:00Z")
        val store = store(useCases = useCases, timePunchUuid = uuid.toString(), new = true)

        val started = store.stateFlow.first { it.timePunch != null }
        assertTrue(actual = started.editing && started.new)
        assertEquals(expected = FakeData.adminUser.uuid, actual = started.timePunch?.userUuid)

        store.send(action = TimePunchDetailScreenAction.ChangeUser(userUuid = FakeData.user.uuid))
        store.send(action = TimePunchDetailScreenAction.ChangeTime(time = time))
        store.send(action = TimePunchDetailScreenAction.Save)

        val saved = assertNotNull(actual = useCases.timePunchUseCases.observeTimePunch(uuid = uuid).first { it != null })
        assertEquals(expected = FakeData.user.uuid, actual = saved.userUuid)
        assertEquals(expected = time, actual = saved.time)
        assertEquals(expected = TimePunch.Source.MANUAL, actual = saved.source)
        assertFalse(actual = store.stateFlow.first { !it.editing }.new)
    }

    /** Verifies that cancelling an edit restores the stored time and stops editing. */
    @Test
    fun cancelDiscardsTheEdits() = runUnitTest {
        val useCases = loggedInUseCases()
        useCases.timePunchUseCases.upsertTimePunch(timePunch = stored)
        val store = store(useCases = useCases, timePunchUuid = stored.uuid.toString())
        store.stateFlow.first { it.timePunch != null }

        store.send(action = TimePunchDetailScreenAction.Edit)
        store.send(action = TimePunchDetailScreenAction.ChangeTime(time = Instant.parse(input = "2022-01-01T00:00:00Z")))
        assertTrue(actual = store.stateFlow.first { it.timePunch?.time != stored.time }.editing)
        store.send(action = TimePunchDetailScreenAction.Cancel)

        val state = store.stateFlow.first { !it.editing }
        assertEquals(expected = stored.time, actual = state.timePunch?.time)
    }
}
