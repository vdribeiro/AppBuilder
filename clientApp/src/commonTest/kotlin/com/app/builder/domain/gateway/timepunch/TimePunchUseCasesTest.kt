package com.app.builder.domain.gateway.timepunch

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.TimePunch
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TimePunchUseCasesTest: TestCase() {

    /** Verifies that upserting a time punch makes it observable both individually and in the time punch list. */
    @Test
    fun crudTimePunch() = runUnitTest {
        val timePunchUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .timePunchUseCases

        assertTrue(actual = timePunchUseCases.observeTimePunches().firstOrNull().orEmpty().isEmpty())
        timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch)
        assertEquals(expected = listOf(element = FakeData.timePunch), actual = timePunchUseCases.observeTimePunches().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.timePunch, actual = timePunchUseCases.observeTimePunch(uuid = FakeData.timePunch.uuid).firstOrNull())
    }

    /** Verifies that the time punch list includes the deleted timePunches. */
    @Test
    fun filterTimePunches() = runUnitTest {
        val timePunchUseCases = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .useCases
            .timePunchUseCases
        val deletedTimePunch = FakeData.timePunch.copy(uuid = uuid(), deletedAt = now())

        timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch)
        timePunchUseCases.upsertTimePunch(timePunch = deletedTimePunch)

        assertEquals(expected = setOf(FakeData.timePunch, deletedTimePunch), actual = timePunchUseCases.observeTimePunches().firstOrNull().orEmpty().toSet())
    }

    /** Verifies that a user without any time punch permission creates its own time punch, but can't change it, delete it or create one for another user. */
    @Test
    fun withoutWritePermission() = runUnitTest {
        // FakeData.account has no time punch permission, and owns FakeData.timePunch.
        val timePunchUseCases = dependency.get()
            .getUserDependency(account = FakeData.account)
            .useCases
            .timePunchUseCases

        timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch)
        timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch.copy(source = TimePunch.Source.NFC))
        timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch.copy(deletedAt = now()))
        timePunchUseCases.upsertTimePunch(timePunch = FakeData.timePunch.copy(uuid = uuid(), userUuid = FakeData.adminUser.uuid))

        assertEquals(expected = listOf(element = FakeData.timePunch), actual = timePunchUseCases.observeTimePunches().firstOrNull().orEmpty())
    }
}
