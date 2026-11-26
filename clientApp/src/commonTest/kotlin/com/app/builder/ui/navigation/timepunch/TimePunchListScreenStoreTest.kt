package com.app.builder.ui.navigation.timepunch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.storage.AppFile
import com.app.builder.domain.TimePunch
import com.app.builder.domain.TimePunch.Property
import com.app.builder.domain.gateway.UseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TimePunchListScreenStoreTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, scheduler = true) }
    }

    /** A manual time punch of the admin user, in 2020. */
    private val early = TimePunch(
        userUuid = FakeData.adminUser.uuid,
        time = Instant.parse(input = "2020-01-01T09:00:00Z"),
    )

    /** An NFC time punch of the admin user, in 2021. */
    private val late = TimePunch(
        userUuid = FakeData.adminUser.uuid,
        time = Instant.parse(input = "2021-06-01T18:00:00Z"),
        source = TimePunch.Source.NFC,
    )

    /** A deleted time punch of the admin user, which the list only shows on request. */
    private val deleted = TimePunch(
        userUuid = FakeData.adminUser.uuid,
        time = Instant.parse(input = "2019-01-01T09:00:00Z"),
        deletedAt = Instant.parse(input = "2021-01-01T00:00:00Z"),
    )

    /** Every [Property] name, so every property is searchable and visible unless a test narrows it. */
    private val allProperties = Property.entries.map { it.name }

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
     * @return The time punch list store.
     */
    private fun store(useCases: UseCases): TimePunchListScreenStore = TimePunchListScreenStore(
        state = TimePunchListScreenState(),
        router = router,
        timePunchUseCases = useCases.timePunchUseCases,
        userUseCases = useCases.userUseCases,
        authenticationUseCases = useCases.authenticationUseCases
    )

    /** Verifies that the list sorts by time, hides the deleted time punches and shows the user name and source of each one. */
    @Test
    fun listsTimePunchesNewestFirst() = runUnitTest {
        val useCases = loggedInUseCases()
        listOf(early, late, deleted).forEach { useCases.timePunchUseCases.upsertTimePunch(timePunch = it) }
        AppFile.TimePunchPreferences.save {
            AppFile.ActionBarData(sortProperty = Property.TIME.name, sortAscending = false, searchableProperties = allProperties, visibleProperties = allProperties)
        }

        val items = store(useCases = useCases).stateFlow.first { state -> state.timePunches.size == 2 && state.timePunches.all { it.userName != null } }.timePunches

        assertEquals(expected = listOf(late.uuid.toString(), early.uuid.toString()), actual = items.map { it.uuid })
        assertEquals(expected = listOf(FakeData.adminUser.name, FakeData.adminUser.name), actual = items.map { it.userName })
        assertEquals(expected = listOf(true, false), actual = items.map { it.nfc })
    }

    /** Verifies that the list hides the properties that are not visible. */
    @Test
    fun hidesInvisibleProperties() = runUnitTest {
        val useCases = loggedInUseCases()
        useCases.timePunchUseCases.upsertTimePunch(timePunch = early)
        AppFile.TimePunchPreferences.save {
            AppFile.ActionBarData(sortProperty = Property.TIME.name, searchableProperties = allProperties, visibleProperties = listOf(Property.TIME.name))
        }

        val item = store(useCases = useCases).stateFlow.first { it.timePunches.size == 1 }.timePunches.single()

        assertEquals(expected = early.time.toString(), actual = item.time)
        assertNull(actual = item.userName)
        assertNull(actual = item.nfc)
    }

    /** Verifies that punching the clock creates a manual time punch for the current user. */
    @Test
    fun punchCreatesATimePunchForTheCurrentUser() = runUnitTest {
        val useCases = loggedInUseCases()
        val store = store(useCases = useCases)
        store.stateFlow.first { it.userUuid != null }

        store.send(action = TimePunchListScreenAction.Punch)

        val created = useCases.timePunchUseCases.observeTimePunches().first { it.isNotEmpty() }.single()
        assertEquals(expected = FakeData.adminUser.uuid, actual = created.userUuid)
        assertEquals(expected = TimePunch.Source.MANUAL, actual = created.source)
    }
}
