package com.app.builder.domain.gateway.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.security.deriveKey
import com.app.builder.core.security.uuid
import com.app.builder.data.database.SessionSchema
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AuthenticationUseCasesTest: TestCase() {

    /** Verifies that logging in persists the user, account and session and sets them as the current observed user and account. */
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

        val username = FakeData.adminCredentials.username

        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
        assertNull(actual = userDao.getUserByUsername(username = username).awaitAsOneOrNull())
        assertNull(actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())

        val user = authenticationUseCases.login(credentials = FakeData.adminCredentials)
        assertNotNull(actual = user)

        assertEquals(expected = FakeData.adminUser, actual = user)
        assertEquals(expected = FakeData.adminUser, actual = userDao.getUserByUsername(username = username).awaitAsOneOrNull()?.toUser())
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
        assertEquals(expected = FakeData.adminAccount, actual = authenticationUseCases.observeCurrentAccount().firstOrNull())
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

        val username = FakeData.adminCredentials.username

        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
        assertNull(actual = userDao.getUserByUsername(username = username).awaitAsOneOrNull())
        assertNull(actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())

        val salt = uuid().toString()
        userDao.upsertUser(User = FakeData.adminUser.toUserSchema())
        dependency.get().database.accountQueries.upsertAccount(Account = FakeData.adminAccount.toAccountSchema())
        val encryptedBearer = assertNotNull(actual = FakeData.adminBearerToken.encrypt())
        sessionDao.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                username = username,
                salt = salt,
                verifier = deriveKey(password = FakeData.adminCredentials.password, salt = salt)!!,
                accessToken = encryptedBearer.accessToken,
                refreshToken = encryptedBearer.refreshToken,
                expiredAt = null
            )
        )

        val user = authenticationUseCases.login(credentials = FakeData.adminCredentials)

        assertEquals(expected = FakeData.adminUser, actual = user)
        assertEquals(expected = FakeData.adminUser, actual = userDao.getUserByUsername(username = username).awaitAsOneOrNull()?.toUser())
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
        assertEquals(expected = FakeData.adminAccount, actual = authenticationUseCases.observeCurrentAccount().firstOrNull())
    }

    /** Verifies that logging out clears the current observed user and account after a successful login. */
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
        assertNull(actual = authenticationUseCases.observeCurrentAccount().firstOrNull())
    }
}
