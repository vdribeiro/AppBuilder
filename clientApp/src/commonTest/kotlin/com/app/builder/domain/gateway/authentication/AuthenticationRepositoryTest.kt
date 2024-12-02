package com.app.builder.domain.gateway.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.security.hash
import com.app.builder.data.database.SessionSchema
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.scheduler.Job
import com.app.builder.domain.scheduler.JobResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AuthenticationRepositoryTest: TestCase() {

    /** Verifies that executing a login job fails without a payload and succeeds once credentials are encoded into it. */
    @Test
    fun executeLogin() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        val authenticationRepository = dependency.get()
            .userGraph
            .repositories
            .authenticationRepository
        val userDao = dependency.get()
            .database
            .userQueries
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        val hash = hash(content = "${FakeData.adminCredentials.username}${FakeData.adminCredentials.password}")!!

        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
        assertNull(actual = userDao.getUserBySessionHash(hash = hash).awaitAsOneOrNull())
        assertNull(actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.SESSION, type = Job.Type.POST)
        assertTrue(actual = authenticationRepository.executeLogin(job = job) is JobResult.Error)

        val jobResult = authenticationRepository.executeLogin(job = job.copy(payload = encode(value = FakeData.adminCredentials)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.adminUser, actual = userDao.getUserBySessionHash(hash = hash).awaitAsOneOrNull()?.toUser())
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
    }

    /** Verifies that executing a login job with a valid payload fails when the http client flag is disabled. */
    @Test
    fun executeLoginOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val authenticationRepository = dependency.get()
            .userGraph
            .repositories
            .authenticationRepository

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.SESSION,
            type = Job.Type.POST,
            payload = encode(value = FakeData.adminCredentials)
        )
        val jobResult = authenticationRepository.executeLogin(job = job)
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that executing a logout job fails without a payload and succeeds once the user uuid is set, removing the session. */
    @Test
    fun executeLogout() = runUnitTest {
        val authenticationRepository = dependency.get()
            .userGraph
            .repositories
            .authenticationRepository
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        assertNull(actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                hash = "hash",
                accessToken = FakeData.adminBearerToken.accessToken,
                refreshToken = FakeData.adminBearerToken.refreshToken
            )
        )
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.SESSION, type = Job.Type.DELETE)
        assertTrue(actual = authenticationRepository.executeLogout(job = job) is JobResult.Error)

        val jobResult = authenticationRepository.executeLogout(job = job.copy(payload = FakeData.adminUser.uuid.toString()))
        assertTrue(actual = jobResult is JobResult.Success)

        assertNull(actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())
    }

    /** Verifies that executing a logout job fails when the http client flag is disabled. */
    @Test
    fun executeLogoutOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val authenticationRepository = dependency.get()
            .userGraph
            .repositories
            .authenticationRepository
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                hash = "hash",
                accessToken = FakeData.adminBearerToken.accessToken,
                refreshToken = FakeData.adminBearerToken.refreshToken
            )
        )

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.SESSION,
            type = Job.Type.DELETE,
            payload = FakeData.adminUser.uuid.toString()
        )
        val jobResult = authenticationRepository.executeLogout(job = job)
        assertTrue(actual = jobResult is JobResult.Error)
    }

    /** Verifies that retrieving the bearer token returns null with no session and the stored token once a session exists. */
    @Test
    fun getBearerToken() = runUnitTest {
        val authenticationRepository = dependency.get()
            .userGraph
            .repositories
            .authenticationRepository
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        assertNull(actual = authenticationRepository.getBearerToken(userUuid = FakeData.adminUser.uuid))
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                hash = "hash",
                accessToken = FakeData.adminBearerToken.accessToken,
                refreshToken = FakeData.adminBearerToken.refreshToken
            )
        )
        assertEquals(expected = FakeData.adminBearerToken, actual = authenticationRepository.getBearerToken(userUuid = FakeData.adminUser.uuid))
    }

    /** Verifies that refreshing the bearer token returns null with no session and the refreshed token once a session exists. */
    @Test
    fun refreshBearerToken() = runUnitTest {
        val authenticationRepository = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .repositories
            .authenticationRepository
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        assertNull(actual = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.adminBearerToken.refreshToken))

        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                hash = "hash",
                accessToken = FakeData.adminBearerToken.accessToken,
                refreshToken = FakeData.adminBearerToken.refreshToken
            )
        )

        val bearer = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.adminBearerToken.refreshToken)
        assertEquals(expected = FakeData.adminBearerToken, actual = bearer)
    }

    /** Verifies that refreshing the bearer token returns null when the http client flag is disabled. */
    @Test
    fun refreshBearerTokenOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val authenticationRepository = dependency.get()
            .userGraph
            .repositories
            .authenticationRepository
        assertNull(actual = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.adminBearerToken.refreshToken))
    }
}
