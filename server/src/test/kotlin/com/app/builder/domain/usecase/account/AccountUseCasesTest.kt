package com.app.builder.domain.usecase.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.domain.DeviceToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.usecase.WriteResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AccountUseCasesTest: TestCase() {

    /** Verifies that an existing account can be retrieved by user uuid and updated, and that a missing one is neither found nor created. */
    @Test
    fun upsertAndGetAccount() = runServerTest {
        val accountUseCases = dependency.get()
            .useCases
            .accountUseCases

        assertEquals(expected = FakeData.account, actual = accountUseCases.getAccount(userUuid = FakeData.user.uuid))
        assertNull(actual = accountUseCases.getAccount(userUuid = uuid()))

        val updated = FakeData.account.copy(username = "renamed", permissions = mapOf(EntityType.TASK to Permission.WRITE))
        assertEquals(expected = WriteResult.SUCCESS, actual = accountUseCases.upsertAccount(account = updated))
        assertEquals(expected = updated, actual = accountUseCases.getAccount(userUuid = FakeData.user.uuid))

        val missing = FakeData.account.copy(userUuid = uuid(), username = "missing")
        assertEquals(expected = WriteResult.NOT_FOUND, actual = accountUseCases.upsertAccount(account = missing))
        assertNull(actual = accountUseCases.getAccount(userUuid = missing.userUuid))
    }

    /** Verifies that updating an account leaves its password unchanged. */
    @Test
    fun upsertAccountKeepsPassword() = runServerTest {
        val useCases = dependency.get().useCases

        assertEquals(expected = WriteResult.SUCCESS, actual = useCases.accountUseCases.upsertAccount(account = FakeData.account.copy(username = "renamed")))
        assertEquals(
            expected = FakeData.account.copy(username = "renamed"),
            actual = useCases.authenticationUseCases.login(credentials = UserCredentials(username = "renamed", password = FakeData.credentials.password))?.account
        )
    }

    /** Verifies that updating an account to a username already taken by another account fails. */
    @Test
    fun upsertAccountRejectsTakenUsername() = runServerTest {
        val accountUseCases = dependency.get()
            .useCases
            .accountUseCases

        assertEquals(expected = WriteResult.CONFLICT, actual = accountUseCases.upsertAccount(account = FakeData.account.copy(username = FakeData.adminAccount.username)))
        assertEquals(expected = FakeData.account, actual = accountUseCases.getAccount(userUuid = FakeData.user.uuid))
    }

    /**
     * Verifies that disabling an account purges its user's device tokens in the same operation.
     * Registering a new one is already refused, so without this the account would keep receiving pushes on whatever it had registered before it was disabled.
     */
    @Test
    fun upsertAccountPurgesDeviceTokensOnDisable() = runServerTest {
        val useCases = dependency.get().useCases
        val accountUseCases = useCases.accountUseCases
        val deviceTokenUseCases = useCases.deviceTokenUseCases

        assertTrue(actual = deviceTokenUseCases.registerToken(userUuid = FakeData.user.uuid, registration = DeviceToken(deviceUuid = uuid(), token = "fcmToken")))
        assertEquals(expected = 1, actual = deviceTokenUseCases.getTokens(userUuid = FakeData.user.uuid).size)

        // An ordinary update leaves them alone.
        assertEquals(expected = WriteResult.SUCCESS, actual = accountUseCases.upsertAccount(account = FakeData.account.copy(username = "renamed")))
        assertEquals(expected = 1, actual = deviceTokenUseCases.getTokens(userUuid = FakeData.user.uuid).size)

        assertEquals(expected = WriteResult.SUCCESS, actual = accountUseCases.upsertAccount(account = FakeData.account.copy(deletedAt = now())))
        assertTrue(actual = deviceTokenUseCases.getTokens(userUuid = FakeData.user.uuid).isEmpty())
    }

    /** Verifies that fetching accounts since a last sync timestamp only returns accounts modified after that time, and paginates using the cursor timestamp and user uuid. */
    @Test
    fun getAccountsPaginated() = runServerTest {
        val accountUseCases = dependency.get()
            .useCases
            .accountUseCases

        val now = now()
        val old = FakeData.account.copy(modifiedAt = now - 10.minutes)
        val recent = FakeData.adminAccount.copy(modifiedAt = now)
        assertEquals(expected = WriteResult.SUCCESS, actual = accountUseCases.upsertAccount(account = old))
        assertEquals(expected = WriteResult.SUCCESS, actual = accountUseCases.upsertAccount(account = recent))

        assertEquals(expected = setOf(old, recent), actual = accountUseCases.getAccounts(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).toSet())
        assertEquals(expected = listOf(recent), actual = accountUseCases.getAccounts(lastSyncUtc = now - 1.minutes, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize))

        val firstPage = accountUseCases.getAccounts(lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0), cursorUtc = null, cursorUuid = null, pageSize = 1)
        assertEquals(expected = listOf(recent), actual = firstPage)
        val secondPage = accountUseCases.getAccounts(
            lastSyncUtc = Instant.fromEpochSeconds(epochSeconds = 0),
            cursorUtc = firstPage.last().modifiedAt,
            cursorUuid = firstPage.last().userUuid,
            pageSize = 1
        )
        assertEquals(expected = listOf(old), actual = secondPage)
    }
}
