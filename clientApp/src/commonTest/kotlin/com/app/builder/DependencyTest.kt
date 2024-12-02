package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.Dependency.getUserDependency
import com.app.builder.data.database.NoOpSqlDriver
import com.app.builder.data.http.NoOpHttpClientEngine
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
}
