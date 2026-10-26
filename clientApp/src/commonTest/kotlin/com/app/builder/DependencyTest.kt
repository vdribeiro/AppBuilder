package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import com.app.builder.Dependency.getUserDependency
import com.app.builder.Dependency.getUserGraph
import com.app.builder.core.security.uuid
import com.app.builder.data.database.NoOpSqlDriver
import com.app.builder.data.http.NoOpHttpClientEngine
import com.app.builder.domain.gateway.authentication.toUserSchema
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class DependencyTest: TestCase() {

    /** Verifies that a user graph retains the original app graph and carries the given user uuid. */
    @Test
    fun noOpDependency() = runUnitTest {
        val dependency = Dependency.AppGraph(
            sqlDriver = NoOpSqlDriver,
            httpClientEngine = NoOpHttpClientEngine
        )
        val userGraph = dependency.getUserDependency(user = FakeData.adminUser)

        assertEquals(expected = FakeData.adminUser.uuid, actual = userGraph.user.uuid)
        assertEquals(expected = dependency, actual = userGraph.dependency)
    }

    /** Verifies that a null or guest uuid resolves to the guest graph, a stored user to their own graph, and an unknown user to nothing. */
    @Test
    fun getUserGraph() = runUnitTest {
        val appGraph = dependency.get()
        appGraph.database.userQueries.upsertUser(User = FakeData.user.toUserSchema())

        assertSame(expected = appGraph.userGraph, actual = appGraph.getUserGraph(userUuid = null))
        assertSame(expected = appGraph.userGraph, actual = appGraph.getUserGraph(userUuid = Dependency.guestUser.uuid))
        assertEquals(expected = FakeData.user.uuid, actual = appGraph.getUserGraph(userUuid = FakeData.user.uuid)?.user?.uuid)
        assertNull(actual = appGraph.getUserGraph(userUuid = uuid()))
    }
}
