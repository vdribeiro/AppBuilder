package com.app.builder.domain.gateway.user

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class UserUseCasesTest: TestCase() {

    /** Verifies that upserting a user makes it observable both individually and in the user list. */
    @Test
    fun crudUser() = runUnitTest {
        val userUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .userUseCases

        assertTrue(actual = userUseCases.observeUsers().firstOrNull().orEmpty().isEmpty())
        userUseCases.upsertUser(user = FakeData.adminUser)
        assertEquals(expected = listOf(element = FakeData.adminUser), actual = userUseCases.observeUsers().firstOrNull().orEmpty())
        assertEquals(expected = FakeData.adminUser, actual = userUseCases.observeUser(uuid = FakeData.adminUser.uuid).firstOrNull())
    }
}
