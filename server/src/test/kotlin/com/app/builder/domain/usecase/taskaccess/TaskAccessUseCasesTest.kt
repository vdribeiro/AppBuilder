package com.app.builder.domain.usecase.taskaccess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.firstOrNull
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.security.uuid
import com.app.builder.domain.usecase.WriteResult
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TaskAccessUseCasesTest: TestCase() {

    /** The lower bound that makes every access part of a sync. */
    private val epoch = Instant.fromEpochSeconds(epochSeconds = 0)

    /** Verifies that creating a task grants its creator an access stamped with the task's modifiedAt. */
    @Test
    fun upsertTaskGrantsCreatorAccess() = runServerTest {
        val useCases = dependency.get().useCases

        assertTrue(actual = useCases.taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = FakeData.task))

        val access = assertNotNull(actual = useCases.taskAccessUseCases.getTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.adminUser.uuid))
        assertEquals(expected = FakeData.task.modifiedAt, actual = access.modifiedAt)
        assertNull(actual = access.deletedAt)
        assertEquals(expected = listOf(access), actual = useCases.taskAccessUseCases.getTaskAccesses(userUuid = FakeData.adminUser.uuid, lastSyncUtc = epoch, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize))
    }

    /** Verifies that granting an access shares the task and notifies the user, and revoking it hides the task while the user still syncs its own revoked access. */
    @Test
    fun grantAndRevokeAccess() = runServerTest {
        val useCases = dependency.get().useCases
        val taskUseCases = useCases.taskUseCases
        val taskAccessUseCases = useCases.taskAccessUseCases

        assertTrue(actual = taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = FakeData.task))
        assertTrue(actual = useCases.notificationsUseCases.observeNotifications(userUuid = FakeData.user.uuid).firstOrNull().orEmpty().isEmpty())

        assertEquals(expected = WriteResult.SUCCESS, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = FakeData.taskAccess))
        assertTrue(actual = useCases.notificationsUseCases.observeNotifications(userUuid = FakeData.user.uuid).firstOrNull().orEmpty().isNotEmpty())
        assertEquals(expected = FakeData.task, actual = taskUseCases.getTask(userUuid = FakeData.user.uuid, uuid = FakeData.task.uuid))
        assertEquals(expected = listOf(FakeData.task), actual = taskUseCases.getTasks(userUuid = FakeData.user.uuid, lastSyncUtc = epoch, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize))
        assertEquals(expected = 2, actual = taskAccessUseCases.getTaskAccesses(userUuid = FakeData.user.uuid, lastSyncUtc = epoch, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).size)

        val revoked = FakeData.taskAccess.copy(modifiedAt = FakeData.taskAccess.modifiedAt + 1.seconds, deletedAt = FakeData.taskAccess.modifiedAt + 1.seconds)
        assertEquals(expected = WriteResult.SUCCESS, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = revoked))
        assertNull(actual = taskUseCases.getTask(userUuid = FakeData.user.uuid, uuid = FakeData.task.uuid))
        assertTrue(actual = taskUseCases.getTasks(userUuid = FakeData.user.uuid, lastSyncUtc = epoch, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).isEmpty())
        assertFalse(actual = taskUseCases.upsertTask(userUuid = FakeData.user.uuid, task = FakeData.task.copy(title = "Revoked")))
        assertEquals(expected = listOf(revoked), actual = taskAccessUseCases.getTaskAccesses(userUuid = FakeData.user.uuid, lastSyncUtc = epoch, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize))

        val regranted = revoked.copy(uuid = uuid(), modifiedAt = revoked.modifiedAt + 1.seconds, deletedAt = null)
        assertEquals(expected = WriteResult.SUCCESS, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = regranted))
        assertEquals(expected = regranted.copy(uuid = FakeData.taskAccess.uuid), actual = taskAccessUseCases.getTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.user.uuid))
        assertEquals(expected = FakeData.task, actual = taskUseCases.getTask(userUuid = FakeData.user.uuid, uuid = FakeData.task.uuid))
    }

    /** Verifies that a user can revoke its own access, losing the task. */
    @Test
    fun revokeOwnAccess() = runServerTest {
        val useCases = dependency.get().useCases

        assertTrue(actual = useCases.taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = FakeData.task))
        val own = assertNotNull(actual = useCases.taskAccessUseCases.getTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.adminUser.uuid))
        val revoked = own.copy(modifiedAt = own.modifiedAt + 1.seconds, deletedAt = own.modifiedAt + 1.seconds)

        assertEquals(expected = WriteResult.SUCCESS, actual = useCases.taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = revoked))
        assertNull(actual = useCases.taskUseCases.getTask(userUuid = FakeData.adminUser.uuid, uuid = FakeData.task.uuid))
        assertEquals(expected = WriteResult.NOT_FOUND, actual = useCases.taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = FakeData.taskAccess))
    }

    /** Verifies that an access is refused when the task or the user doesn't exist, or the caller has no active access to the task. */
    @Test
    fun upsertTaskAccessNotFound() = runServerTest {
        val useCases = dependency.get().useCases
        val taskAccessUseCases = useCases.taskAccessUseCases

        assertEquals(expected = WriteResult.NOT_FOUND, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = FakeData.taskAccess))
        assertTrue(actual = useCases.taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = FakeData.task))

        assertEquals(expected = WriteResult.NOT_FOUND, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.user.uuid, access = FakeData.taskAccess))
        assertEquals(expected = WriteResult.NOT_FOUND, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = FakeData.taskAccess.copy(userUuid = uuid())))
        assertNull(actual = taskAccessUseCases.getTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.user.uuid))
        assertTrue(actual = useCases.notificationsUseCases.observeNotifications(userUuid = FakeData.user.uuid).firstOrNull().orEmpty().isEmpty())
    }

    /** Verifies that an older write leaves the newer stored access unchanged, still succeeding without notifying. */
    @Test
    fun upsertTaskAccessLastWriteWins() = runServerTest {
        val useCases = dependency.get().useCases
        val taskAccessUseCases = useCases.taskAccessUseCases

        assertTrue(actual = useCases.taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = FakeData.task))
        val granted = FakeData.taskAccess.copy(modifiedAt = FakeData.taskAccess.modifiedAt + 10.seconds)
        assertEquals(expected = WriteResult.SUCCESS, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = granted))

        val staleRevoke = FakeData.taskAccess.copy(modifiedAt = FakeData.taskAccess.modifiedAt + 5.seconds, deletedAt = FakeData.taskAccess.modifiedAt + 5.seconds)
        assertEquals(expected = WriteResult.SUCCESS, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = staleRevoke))
        assertEquals(expected = granted, actual = taskAccessUseCases.getTaskAccess(taskUuid = FakeData.task.uuid, userUuid = FakeData.user.uuid))
    }

    /** Verifies that a new access reusing the uuid of another access is refused. */
    @Test
    fun upsertTaskAccessConflict() = runServerTest {
        val useCases = dependency.get().useCases
        val otherTask = FakeData.task.copy(uuid = uuid())

        assertTrue(actual = useCases.taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = FakeData.task))
        assertTrue(actual = useCases.taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = otherTask))
        assertEquals(expected = WriteResult.SUCCESS, actual = useCases.taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = FakeData.taskAccess))

        assertEquals(expected = WriteResult.CONFLICT, actual = useCases.taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = FakeData.taskAccess.copy(taskUuid = otherTask.uuid)))
        assertNull(actual = useCases.taskAccessUseCases.getTaskAccess(taskUuid = otherTask.uuid, userUuid = FakeData.user.uuid))
    }

    /** Verifies that fetching accesses only returns those modified after the last sync, hides other tasks' accesses, and paginates by the modifiedAt/uuid cursor. */
    @Test
    fun getTaskAccessesDeltaSync() = runServerTest {
        val useCases = dependency.get().useCases
        val taskAccessUseCases = useCases.taskAccessUseCases
        val oldTask = FakeData.task.copy(modifiedAt = FakeData.task.modifiedAt - 10.minutes)
        val hiddenTask = FakeData.task.copy(uuid = uuid())

        assertTrue(actual = useCases.taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = oldTask))
        assertTrue(actual = useCases.taskUseCases.upsertTask(userUuid = FakeData.adminUser.uuid, task = hiddenTask))
        val recent = FakeData.taskAccess.copy(modifiedAt = FakeData.task.modifiedAt)
        assertEquals(expected = WriteResult.SUCCESS, actual = taskAccessUseCases.upsertTaskAccess(userUuid = FakeData.adminUser.uuid, access = recent))
        val creator = assertNotNull(actual = taskAccessUseCases.getTaskAccess(taskUuid = oldTask.uuid, userUuid = FakeData.adminUser.uuid))

        assertEquals(expected = setOf(recent, creator), actual = taskAccessUseCases.getTaskAccesses(userUuid = FakeData.user.uuid, lastSyncUtc = epoch, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).toSet())
        assertEquals(expected = listOf(recent), actual = taskAccessUseCases.getTaskAccesses(userUuid = FakeData.user.uuid, lastSyncUtc = recent.modifiedAt - 1.minutes, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize))

        val firstPage = taskAccessUseCases.getTaskAccesses(userUuid = FakeData.user.uuid, lastSyncUtc = epoch, cursorUtc = null, cursorUuid = null, pageSize = 1)
        assertEquals(expected = listOf(recent), actual = firstPage)
        val secondPage = taskAccessUseCases.getTaskAccesses(
            userUuid = FakeData.user.uuid,
            lastSyncUtc = epoch,
            cursorUtc = firstPage.last().modifiedAt,
            cursorUuid = firstPage.last().uuid,
            pageSize = 1
        )
        assertEquals(expected = listOf(creator), actual = secondPage)
        assertEquals(expected = 3, actual = taskAccessUseCases.getTaskAccesses(userUuid = FakeData.adminUser.uuid, lastSyncUtc = epoch, cursorUtc = null, cursorUuid = null, pageSize = ServerConfigs.value.pageSize).size)
    }
}
