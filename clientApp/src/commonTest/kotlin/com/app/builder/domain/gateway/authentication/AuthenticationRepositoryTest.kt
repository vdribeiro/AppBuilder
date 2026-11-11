package com.app.builder.domain.gateway.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import io.ktor.http.HttpStatusCode
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.Dependency
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.data.database.SessionSchema
import com.app.builder.data.database.createMockSqlDriver
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.respondMock
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

        val username = FakeData.adminCredentials.username

        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
        assertNull(actual = userDao.getUserByUsername(username = username).awaitAsOneOrNull())
        assertNull(actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.ACCOUNT, type = Job.Type.POST)
        assertTrue(actual = authenticationRepository.executeLogin(job = job) is JobResult.Error)

        val jobResult = authenticationRepository.executeLogin(job = job.copy(payload = encode(value = FakeData.adminCredentials)))
        assertTrue(actual = jobResult is JobResult.Success)

        assertEquals(expected = FakeData.adminUser, actual = userDao.getUserByUsername(username = username).awaitAsOneOrNull()?.toUser())
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
            entityType = EntityType.ACCOUNT,
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
        val encryptedBearer = assertNotNull(actual = FakeData.adminBearerToken.encrypt())
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                username = FakeData.adminCredentials.username,
                salt = "salt",
                verifier = "verifier",
                accessToken = encryptedBearer.accessToken,
                refreshToken = encryptedBearer.refreshToken,
                expiredAt = null
            )
        )
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.ACCOUNT, type = Job.Type.DELETE)
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

        val encryptedBearer = assertNotNull(actual = FakeData.adminBearerToken.encrypt())
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                username = FakeData.adminCredentials.username,
                salt = "salt",
                verifier = "verifier",
                accessToken = encryptedBearer.accessToken,
                refreshToken = encryptedBearer.refreshToken,
                expiredAt = null
            )
        )

        val job = Job(
            userUuid = Uuid.NIL,
            entityType = EntityType.ACCOUNT,
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
        val encryptedBearer = assertNotNull(actual = FakeData.adminBearerToken.encrypt())
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                username = FakeData.adminCredentials.username,
                salt = "salt",
                verifier = "verifier",
                accessToken = encryptedBearer.accessToken,
                refreshToken = encryptedBearer.refreshToken,
                expiredAt = null
            )
        )
        assertEquals(expected = FakeData.adminBearerToken, actual = authenticationRepository.getBearerToken(userUuid = FakeData.adminUser.uuid))
    }

    /** Verifies that refreshing the bearer token returns null with no session and the refreshed token once a session exists. */
    @Test
    fun refreshBearerToken() = runUnitTest {
        val authenticationRepository = dependency.get()
            .getUserDependency(account = FakeData.adminAccount)
            .repositories
            .authenticationRepository
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        assertNull(actual = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.adminBearerToken.refreshToken))

        val encryptedBearer = assertNotNull(actual = FakeData.adminBearerToken.encrypt())
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                username = FakeData.adminCredentials.username,
                salt = "salt",
                verifier = "verifier",
                accessToken = encryptedBearer.accessToken,
                refreshToken = encryptedBearer.refreshToken,
                expiredAt = null
            )
        )

        val bearer = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.adminBearerToken.refreshToken)
        assertEquals(expected = FakeData.adminBearerToken, actual = bearer)
    }

    /** Verifies that a refresh token the server rejects returns null, keeping the session and the current user. */
    @Test
    fun refreshBearerTokenRejected() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        val authenticationRepository = dependency.get()
            .userGraph
            .repositories
            .authenticationRepository
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        assertNotNull(actual = authenticationUseCases.login(credentials = FakeData.adminCredentials))
        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)

        assertEquals(expected = false, actual = authenticationUseCases.observeSessionExpired().first())
        assertNull(actual = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.expiredBearerToken.refreshToken))

        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)
        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
        assertEquals(expected = true, actual = authenticationUseCases.observeSessionExpired().first())

        assertNotNull(actual = authenticationUseCases.login(credentials = FakeData.adminCredentials))
        assertEquals(expected = false, actual = authenticationUseCases.observeSessionExpired().first())
    }

    /** Verifies that a session counts as active only while it exists and the server has not refused it, and that an expired one is not refreshed again. */
    @Test
    fun expiredSessionIsNotRefreshed() = runUnitTest {
        val userGraph = dependency.get().userGraph
        val authenticationRepository = userGraph.repositories.authenticationRepository

        assertFalse(actual = authenticationRepository.hasActiveSession(userUuid = FakeData.adminUser.uuid))
        assertNotNull(actual = userGraph.useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials))
        assertTrue(actual = authenticationRepository.hasActiveSession(userUuid = FakeData.adminUser.uuid))

        assertNull(actual = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.expiredBearerToken.refreshToken))
        assertFalse(actual = authenticationRepository.hasActiveSession(userUuid = FakeData.adminUser.uuid))
        assertNull(actual = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.adminBearerToken.refreshToken))
    }

    /** Verifies that logging out of a session the server already refused completes and removes the local session. */
    @Test
    fun executeLogoutOfARefusedSession() = runUnitTest {
        val appGraph = Dependency.AppGraph(
            sqlDriver = createMockSqlDriver(),
            httpClientEngine = TestEngine { respondMock(status = HttpStatusCode.Unauthorized) }
        )
        val authenticationRepository = appGraph.userGraph.repositories.authenticationRepository
        val sessionDao = appGraph.database.sessionQueries
        val encryptedBearer = assertNotNull(actual = FakeData.expiredBearerToken.encrypt())
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                username = FakeData.adminCredentials.username,
                salt = "salt",
                verifier = "verifier",
                accessToken = encryptedBearer.accessToken,
                refreshToken = encryptedBearer.refreshToken,
                expiredAt = null
            )
        )

        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.ACCOUNT, type = Job.Type.DELETE, payload = FakeData.adminUser.uuid.toString())
        assertTrue(actual = authenticationRepository.executeLogout(job = job) is JobResult.Success)
        assertNull(actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())
    }

    /** Verifies that refreshing the bearer token returns null when the http client flag is disabled, keeping the session and the current user. */
    @Test
    fun refreshBearerTokenOffline() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        val authenticationRepository = dependency.get()
            .userGraph
            .repositories
            .authenticationRepository
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        assertNotNull(actual = authenticationUseCases.login(credentials = FakeData.adminCredentials))
        ClientFlags.set { it.copy(http = false) }

        assertNull(actual = authenticationRepository.refreshBearerToken(userUuid = FakeData.adminUser.uuid, refreshToken = FakeData.adminBearerToken.refreshToken))

        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)
        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
        assertEquals(expected = false, actual = authenticationUseCases.observeSessionExpired().first())
    }
}
