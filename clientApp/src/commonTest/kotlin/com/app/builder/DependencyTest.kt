package com.app.builder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import com.app.builder.Dependency.getUserDependency
import com.app.builder.Dependency.getUserGraph
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.NoOpSqlDriver
import com.app.builder.data.http.NoOpHttpClientEngine
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.gateway.authentication.toAccountSchema
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
        val userGraph = dependency.getUserDependency(account = FakeData.adminAccount)

        assertEquals(expected = FakeData.adminUser.uuid, actual = userGraph.account.userUuid)
        assertEquals(expected = dependency, actual = userGraph.dependency)
    }

    /** Verifies that a null or guest uuid resolves to the guest graph, a user with a stored account to their own graph, and an unknown user to nothing. */
    @Test
    fun getUserGraph() = runUnitTest {
        val appGraph = dependency.get()
        appGraph.database.accountQueries.upsertAccount(Account = FakeData.account.toAccountSchema())

        assertSame(expected = appGraph.userGraph, actual = appGraph.getUserGraph(userUuid = null))
        assertSame(expected = appGraph.userGraph, actual = appGraph.getUserGraph(userUuid = Dependency.guestAccount.userUuid))
        assertEquals(expected = FakeData.user.uuid, actual = appGraph.getUserGraph(userUuid = FakeData.user.uuid)?.account?.userUuid)
        assertNull(actual = appGraph.getUserGraph(userUuid = uuid()))
    }

    /** Verifies that a user graph is reused while the account's permissions and disabled state are unchanged, and rebuilt with the new account when either changes. */
    @Test
    fun getUserDependencyRebuildsOnAccessChange() = runUnitTest {
        val appGraph = dependency.get()
        val userGraph = appGraph.getUserDependency(account = FakeData.account)

        assertSame(expected = userGraph, actual = appGraph.getUserDependency(account = FakeData.account.copy(username = "renamed", modifiedAt = now())))

        val granted = FakeData.account.copy(permissions = FakeData.account.permissions + (EntityType.TASK to Permission.WRITE))
        val grantedGraph = appGraph.getUserDependency(account = granted)
        assertNotSame(illegal = userGraph, actual = grantedGraph)
        assertEquals(expected = granted, actual = grantedGraph.account)

        val disabled = granted.copy(deletedAt = now())
        assertNotSame(illegal = grantedGraph, actual = appGraph.getUserDependency(account = disabled))
    }
}
