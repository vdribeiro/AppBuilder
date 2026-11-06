package com.app.builder.domain.gateway.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AccountGatewayTest: TestCase() {

    /** An account allowed to read accounts but not to create or update them. */
    private val readOnlyAccount: Account = FakeData.account.copy(permissions = mapOf(EntityType.ACCOUNT to Permission.READ))

    /** Verifies that creating an account stores it and the user it belongs to, so both become observable. */
    @Test
    fun createAccount() = runUnitTest {
        val useCases = dependency.get().getUserDependency(account = FakeData.adminAccount).useCases

        assertNull(actual = useCases.accountUseCases.observeAccount(userUuid = FakeData.user.uuid).firstOrNull())

        val account = useCases.accountUseCases.createAccount(registrationForm = FakeData.registrationForm)
        assertEquals(expected = FakeData.account, actual = account)

        assertEquals(expected = FakeData.account, actual = useCases.accountUseCases.observeAccount(userUuid = FakeData.user.uuid).firstOrNull())
        assertEquals(expected = FakeData.user, actual = useCases.userUseCases.observeUser(uuid = FakeData.user.uuid).firstOrNull())
    }

    /** Verifies that updating an account stores the updated account returned by the server. */
    @Test
    fun updateAccount() = runUnitTest {
        val accountUseCases = dependency.get().getUserDependency(account = FakeData.adminAccount).useCases.accountUseCases
        val disabled = FakeData.account.copy(deletedAt = FakeData.account.modifiedAt)

        assertEquals(expected = disabled, actual = accountUseCases.updateAccount(account = disabled))
        assertEquals(expected = disabled, actual = accountUseCases.observeAccount(userUuid = FakeData.user.uuid).firstOrNull())
    }

    /** Verifies that creating and updating an account fail without a connection, since both are online calls. */
    @Test
    fun createAndUpdateAccountOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val accountUseCases = dependency.get().getUserDependency(account = FakeData.adminAccount).useCases.accountUseCases

        assertNull(actual = accountUseCases.createAccount(registrationForm = FakeData.registrationForm))
        assertNull(actual = accountUseCases.updateAccount(account = FakeData.account))
        assertNull(actual = accountUseCases.observeAccount(userUuid = FakeData.user.uuid).firstOrNull())
    }

    /** Verifies that an account without write permission can neither create nor update accounts. */
    @Test
    fun withoutWritePermission() = runUnitTest {
        val accountUseCases = dependency.get().getUserDependency(account = readOnlyAccount).useCases.accountUseCases

        assertNull(actual = accountUseCases.createAccount(registrationForm = FakeData.registrationForm))
        assertNull(actual = accountUseCases.updateAccount(account = FakeData.account))
    }

    /** Verifies that an account without read permission observes no account and fails the fetch job. */
    @Test
    fun withoutReadPermission() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = FakeData.account.copy(permissions = emptyMap()))
        val job = Job(userUuid = Uuid.NIL, entityUuid = FakeData.user.uuid, entityType = EntityType.ACCOUNT, type = Job.Type.GET)

        assertNull(actual = userDependency.useCases.accountUseCases.observeAccount(userUuid = FakeData.user.uuid).firstOrNull())
        assertTrue(actual = userDependency.repositories.accountRepository.executeSyncAccount(job = job) is JobResult.Error)
    }

    /** Verifies that executing an account fetch job fails without a user uuid, and otherwise stores the fetched account. */
    @Test
    fun executeSyncAccount() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = readOnlyAccount)
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.ACCOUNT, type = Job.Type.GET)
        assertTrue(actual = userDependency.repositories.accountRepository.executeSyncAccount(job = job) is JobResult.Error)

        assertTrue(actual = userDependency.repositories.accountRepository.executeSyncAccount(job = job.copy(entityUuid = FakeData.user.uuid)) is JobResult.Success)
        assertEquals(expected = FakeData.account, actual = userDependency.useCases.accountUseCases.observeAccount(userUuid = FakeData.user.uuid).firstOrNull())
    }

    /** Verifies that observing an account queues a job fetching it. */
    @Test
    fun observeAccountQueuesFetch() = runUnitTest {
        val userDependency = dependency.get().getUserDependency(account = readOnlyAccount)

        userDependency.useCases.accountUseCases.observeAccount(userUuid = FakeData.user.uuid).firstOrNull()

        val jobs = dependency.get().database.jobQueries.getJobsByState(states = Job.State.entries).awaitAsList()
        assertTrue(actual = jobs.any { it.entityType == EntityType.ACCOUNT && it.type == Job.Type.GET && it.entityUuid == FakeData.user.uuid })
    }
}
