package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import com.app.builder.Application.createDependency
import com.app.builder.Dependency.getUserDependency
import com.app.builder.data.database.NoOpSqlDriver
import com.app.builder.data.database.createMockSqlDriver
import com.app.builder.data.http.NoOpHttpClientEngine
import com.app.builder.data.http.createMockHttpEngine
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class ApplicationTest: TestCase() {

    /**
     * Starts the [Application] on the test graph, runs [block], and stops it afterwards.
     *
     * @param block The test body, run once the application has initialized.
     */
    private suspend fun withApplication(block: suspend () -> Unit) {
        Application.appGraph = dependency
        try {
            Application.start()
            assertTrue(actual = Application.await(), message = "the application did not initialize")
            block()
        } finally {
            Application.stop()
        }
    }

    /** Logs in as the admin through the admin's use cases. */
    private suspend fun login() {
        dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .authenticationUseCases
            .login(credentials = FakeData.adminCredentials)
    }

    /** Verifies that the graph is built on the engines the factories create. */
    @Test
    fun createDependencyUsesTheCreatedEngines() = runUnitTest {
        val sqlDriver = createMockSqlDriver()
        val httpEngine = createMockHttpEngine()

        val graph = createDependency(sqlDriverFactory = { sqlDriver }, httpEngineFactory = { httpEngine })

        assertSame(expected = sqlDriver, actual = graph.sqlDriver)
        assertSame(expected = httpEngine, actual = graph.httpClientEngine)
    }

    /** Verifies that the graph falls back to No-Op engines when the factories fail. */
    @Test
    fun createDependencyFallsBackToNoOpEngines() = runUnitTest {
        val graph = createDependency(
            sqlDriverFactory = { error("no sql driver") },
            httpEngineFactory = { error("no http engine") },
        )

        assertSame(expected = NoOpSqlDriver, actual = graph.sqlDriver)
        assertSame(expected = NoOpHttpClientEngine, actual = graph.httpClientEngine)
    }

    /** Verifies that starting sets the app graph without a user graph, and stopping clears it. */
    @Test
    fun startSetsTheAppGraphAndStopClearsIt() = runUnitTest {
        withApplication {
            assertSame(expected = dependency.get(), actual = Application.dependency.value)
            assertNull(actual = Application.userDependency.value)
        }

        assertNull(actual = Application.dependency.value)
        assertNull(actual = Application.userDependency.value)
    }

    /** Verifies that starting an already started application keeps the running graph. */
    @Test
    fun startingTwiceKeepsTheRunningGraph() = runUnitTest {
        withApplication {
            val graph = Application.dependency.value

            Application.start()

            assertSame(expected = graph, actual = Application.dependency.value)
        }
    }

    /** Verifies that logging in sets the user graph of that user, and logging out clears it. */
    @Test
    fun userGraphFollowsTheCurrentUser() = runUnitTest {
        withApplication {
            login()
            val userGraph = Application.userDependency.filterNotNull().first()
            assertEquals(expected = FakeData.adminUser.uuid, actual = userGraph.user.uuid)

            userGraph.useCases.authenticationUseCases.logout(userUuid = userGraph.user.uuid)
            Application.userDependency.first { it == null }
        }
    }

    /** Verifies that resetting clears the signed in user and starts the application again. */
    @Test
    fun resetRestartsSignedOut() = runUnitTest {
        withApplication {
            login()
            Application.userDependency.filterNotNull().first()

            Application.reset()

            assertSame(expected = dependency.get(), actual = Application.dependency.filterNotNull().first())
            assertNull(actual = Application.userDependency.value)
        }
    }
}
