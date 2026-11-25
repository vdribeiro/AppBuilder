package com.app.builder.domain.gateway.authentication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.firstOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.security.deriveKey
import com.app.builder.core.security.uuid
import com.app.builder.data.database.SessionSchema
import com.app.builder.domain.PasswordFailure
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.scheduler.Job
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AuthenticationUseCasesTest: TestCase() {

    override suspend fun beforeTest() {
        super.beforeTest()
        ClientFlags.set { it.copy(http = true, scheduler = true) }
    }

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

        assertEquals(expected = LoginResult.Success(user = FakeData.adminUser), actual = authenticationUseCases.login(credentials = FakeData.adminCredentials))
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

        assertEquals(expected = LoginResult.Success(user = FakeData.adminUser), actual = authenticationUseCases.login(credentials = FakeData.adminCredentials))
        assertEquals(expected = FakeData.adminUser, actual = userDao.getUserByUsername(username = username).awaitAsOneOrNull()?.toUser())
        assertEquals(expected = FakeData.adminUser.uuid, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull()?.userUuid)

        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
        assertEquals(expected = FakeData.adminAccount, actual = authenticationUseCases.observeCurrentAccount().firstOrNull())
        assertTrue(actual = queuedJobs().isEmpty())
    }

    /** Verifies that an offline login queues no job, whether it matches or not, and that a match cancels the user's pending logout and keeps its session. */
    @Test
    fun loginOfflineCancelsPendingLogout() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        val sessionDao = dependency.get()
            .database
            .sessionQueries

        assertIs<LoginResult.Success>(value = authenticationUseCases.login(credentials = FakeData.adminCredentials))
        val session = assertNotNull(actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())

        ClientFlags.set { it.copy(http = false) }
        authenticationUseCases.logout(userUuid = FakeData.adminUser.uuid)
        val logout = queuedJobs().single()
        assertEquals(expected = Job.Type.DELETE, actual = logout.type)
        assertEquals(expected = Job.State.PENDING, actual = logout.state)

        assertEquals(expected = LoginResult.NoConnection, actual = authenticationUseCases.login(credentials = UserCredentials(username = FakeData.adminCredentials.username, password = "wrong_password")))
        assertEquals(expected = LoginResult.NoConnection, actual = authenticationUseCases.login(credentials = FakeData.credentials))
        assertEquals(expected = listOf(logout), actual = queuedJobs())

        assertEquals(expected = LoginResult.Success(user = FakeData.adminUser), actual = authenticationUseCases.login(credentials = FakeData.adminCredentials))
        assertEquals(expected = listOf(logout.copy(state = Job.State.CANCELED)), actual = queuedJobs())
        assertEquals(expected = session, actual = sessionDao.getSession(userUuid = FakeData.adminUser.uuid).awaitAsOneOrNull())
        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)
    }

    /** Verifies that each answer the server refuses a login with maps to its own result, and that none of them logs a user in. */
    @Test
    fun loginFailures() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        assertEquals(expected = LoginResult.WrongCredentials, actual = authenticationUseCases.login(credentials = UserCredentials(username = FakeData.adminCredentials.username, password = "wrong_password")))
        assertEquals(expected = LoginResult.AccountDisabled, actual = authenticationUseCases.login(credentials = UserCredentials(username = "disabled_user", password = "password")))
        assertEquals(expected = LoginResult.TooManyAttempts, actual = authenticationUseCases.login(credentials = UserCredentials(username = "rate_limited_user", password = "password")))
        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
    }

    /** Verifies that a login the server answers and refuses is not tried offline, even when the password matches the stored verifier, while the same login succeeds offline once the server can't be reached. */
    @Test
    fun loginRefusedByTheServerIsNotTriedOffline() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        val credentials = UserCredentials(username = FakeData.adminCredentials.username, password = "old_password")

        val salt = uuid().toString()
        dependency.get().database.userQueries.upsertUser(User = FakeData.adminUser.toUserSchema())
        dependency.get().database.accountQueries.upsertAccount(Account = FakeData.adminAccount.toAccountSchema())
        val encryptedBearer = assertNotNull(actual = FakeData.adminBearerToken.encrypt())
        dependency.get().database.sessionQueries.upsertSession(
            SessionSchema(
                userUuid = FakeData.adminUser.uuid,
                username = credentials.username,
                salt = salt,
                verifier = deriveKey(password = credentials.password, salt = salt)!!,
                accessToken = encryptedBearer.accessToken,
                refreshToken = encryptedBearer.refreshToken,
                expiredAt = null
            )
        )

        assertEquals(expected = LoginResult.WrongCredentials, actual = authenticationUseCases.login(credentials = credentials))
        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())

        ClientFlags.set { it.copy(http = false) }
        assertEquals(expected = LoginResult.Success(user = FakeData.adminUser), actual = authenticationUseCases.login(credentials = credentials))
    }

    /**
     * Lists the jobs queued in the scheduler, in every state.
     *
     * @return The queued jobs, oldest first.
     */
    private suspend fun queuedJobs(): List<database.Job> = dependency.get()
        .database
        .jobQueries
        .getJobsByState(states = Job.State.entries)
        .awaitAsList()

    /** Verifies that changing the password with the current one replaces the session's salt and verifier with ones derived from the new password, keeping its tokens. */
    @Test
    fun changePassword() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        val sessionDao = dependency.get()
            .database
            .sessionQueries
        val userUuid = FakeData.adminUser.uuid

        assertEquals(expected = PasswordResult.Failed, actual = authenticationUseCases.changePassword(userUuid = userUuid, currentPassword = FakeData.adminCredentials.password, newPassword = "new_password"))

        assertIs<LoginResult.Success>(value = authenticationUseCases.login(credentials = FakeData.adminCredentials))
        val session = assertNotNull(actual = sessionDao.getSession(userUuid = userUuid).awaitAsOneOrNull())

        assertEquals(expected = PasswordResult.Success(data = Unit), actual = authenticationUseCases.changePassword(userUuid = userUuid, currentPassword = FakeData.adminCredentials.password, newPassword = "new_password"))

        val updatedSession = assertNotNull(actual = sessionDao.getSession(userUuid = userUuid).awaitAsOneOrNull())
        assertNotEquals(illegal = session.salt, actual = updatedSession.salt)
        assertEquals(expected = deriveKey(password = "new_password", salt = updatedSession.salt), actual = updatedSession.verifier)
        assertEquals(expected = session.copy(salt = updatedSession.salt, verifier = updatedSession.verifier), actual = updatedSession)
    }

    /** Verifies that a password change the server refuses or that has no connection reports why and leaves the session's salt and verifier unchanged, and that one answered by ending the session marks it expired. */
    @Test
    fun changePasswordFailures() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases
        val sessionDao = dependency.get()
            .database
            .sessionQueries
        val userUuid = FakeData.adminUser.uuid

        assertIs<LoginResult.Success>(value = authenticationUseCases.login(credentials = FakeData.adminCredentials))
        val session = assertNotNull(actual = sessionDao.getSession(userUuid = userUuid).awaitAsOneOrNull())

        assertEquals(
            expected = PasswordResult.Refused(failure = PasswordFailure.WrongPassword(remainingAttempts = 2)),
            actual = authenticationUseCases.changePassword(userUuid = userUuid, currentPassword = "wrong_password", newPassword = "new_password")
        )
        assertEquals(
            expected = PasswordResult.Refused(failure = PasswordFailure.TooShort(minLength = 10)),
            actual = authenticationUseCases.changePassword(userUuid = userUuid, currentPassword = FakeData.adminCredentials.password, newPassword = "short")
        )

        ClientFlags.set { it.copy(http = false) }
        assertEquals(expected = PasswordResult.Failed, actual = authenticationUseCases.changePassword(userUuid = userUuid, currentPassword = FakeData.adminCredentials.password, newPassword = "new_password"))

        assertEquals(expected = session, actual = sessionDao.getSession(userUuid = userUuid).awaitAsOneOrNull())

        ClientFlags.set { it.copy(http = true) }
        assertEquals(expected = PasswordResult.SessionEnded, actual = authenticationUseCases.changePassword(userUuid = userUuid, currentPassword = "last_wrong_password", newPassword = "new_password"))
        assertNotNull(actual = sessionDao.getSession(userUuid = userUuid).awaitAsOneOrNull()?.expiredAt)
    }

    /** Verifies that logging out clears the current observed user and account after a successful login. */
    @Test
    fun logout() = runUnitTest {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        assertIs<LoginResult.Success>(value = authenticationUseCases.login(credentials = FakeData.adminCredentials))

        assertEquals(expected = FakeData.adminUser.uuid, actual = authenticationUseCases.observeCurrentUser().firstOrNull()?.uuid)

        authenticationUseCases.logout(userUuid = FakeData.adminUser.uuid)
        assertNull(actual = authenticationUseCases.observeCurrentUser().firstOrNull())
        assertNull(actual = authenticationUseCases.observeCurrentAccount().firstOrNull())
    }
}
