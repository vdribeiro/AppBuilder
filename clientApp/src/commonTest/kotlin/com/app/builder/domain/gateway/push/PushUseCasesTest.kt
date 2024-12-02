package com.app.builder.domain.gateway.push

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import io.ktor.client.engine.mock.MockEngine
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.http.URL
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class PushUseCasesTest: TestCase() {

    /** Verifies that pushing a notification to a specific user succeeds when online. */
    @Test
    fun pushToUser() = runUnitTest {
        val pushUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .pushUseCases

        val result = pushUseCases.push(pushPayload = FakeData.notification)
        assertTrue(actual = result)
    }

    /** Verifies that pushing a notification to a specific user fails when the http client flag is disabled. */
    @Test
    fun pushToUserOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val pushUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .pushUseCases

        val result = pushUseCases.push(pushPayload = FakeData.notification)
        assertFalse(actual = result)
    }

    /** Verifies that broadcasting a push notification succeeds when online. */
    @Test
    fun pushBroadcast() = runUnitTest {
        val pushUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .pushUseCases

        val result = pushUseCases.push(pushPayload = FakeData.notification.copy(userUuid = null))
        assertTrue(actual = result)

        val engine = dependency.get().httpClientEngine as MockEngine
        assertTrue(actual = engine.requestHistory.last().url.encodedPath.startsWith(prefix = URL.Broadcast.path))
    }

    /** Verifies that broadcasting a push notification fails when the http client flag is disabled. */
    @Test
    fun pushBroadcastOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val pushUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .pushUseCases

        val result = pushUseCases.push(pushPayload = FakeData.notification.copy(userUuid = null))
        assertFalse(actual = result)
    }
}
