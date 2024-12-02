package com.app.builder.domain.gateway.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.security.hash
import com.app.builder.data.database.SessionSchema
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AuthenticationUseCasesTest: TestCase() {

    /** Verifies that registering a new user persists the user and session but does not set it as the current user. */
    @Test
    fun registerAndObserveCurrentUser() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
            .authenticationUseCases
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

        val user = authenticationUseCases.register(registrationForm = FakeData.adminRegistrationForm)
        assertNotNull(actual = user)

        assertEquals(expected = FakeData.adminUser, actual = user)
        assertEquals(expected = FakeData.adminUser, actual = userDao.getUserBySessionHash(hash = hash).awaitAsOneOrNull()?.toUser())
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
    }

    /** Verifies that registration returns null when the http client flag is disabled. */
    @Test
    fun registerOffline() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val user = authenticationUseCases.register(registrationForm = FakeData.adminRegistrationForm)
        assertNull(actual = user)
    }

    /** Verifies that logging in persists the user and session and sets it as the current observed user. */
    @Test
    fun loginAndObserveCurrentUser() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
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

        val user = authenticationUseCases.login(credentials = FakeData.adminCredentials)
        assertNotNull(actual = user)

        assertEquals(expected = FakeData.adminUser, actual = user)
        assertEquals(expected = FakeData.adminUser, actual = userDao.getUserBySessionHash(hash = hash).awaitAsOneOrNull()?.toUser())
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
    }

    /** Verifies that logging in offline against a pre-existing local session still resolves and observes the current user. */
    @Test
    fun loginOfflineAndObserveCurrentUser() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
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

        userDao.upsertUser(User = FakeData.adminUser.toUserSchema())
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                hash = hash,
                accessToken = FakeData.adminBearerToken.accessToken,
                refreshToken = FakeData.adminBearerToken.refreshToken
            )
        )

        val user = authenticationUseCases.login(credentials = FakeData.adminCredentials)

        assertEquals(expected = FakeData.adminUser, actual = user)
        assertEquals(expected = FakeData.adminUser, actual = userDao.getUserBySessionHash(hash = hash).awaitAsOneOrNull()?.toUser())
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
    }

    /** Verifies that logging out clears the current observed user after a successful login. */
    @Test
    fun logout() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val user = authenticationUseCases.login(credentials = FakeData.adminCredentials)
        assertNotNull(actual = user)

        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)

        authenticationUseCases.logout(userUuid = FakeData.adminUser.uuid)
        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
    }
}
