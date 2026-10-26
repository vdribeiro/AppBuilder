package com.app.builder.domain.permission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.app.builder.core.security.toUuid
import com.app.builder.data.signal.LocalSignal
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class PermissionServiceTest: TestCase() {

    /** Verifies that permissions set on one instance propagate to another and are cleared after stop. */
    @Test
    fun startSetGetStop() = runServerTest {
        val permissionService = dependency.get().permissionService
        val instanceSignal = dependency.get().instanceSignal as LocalSignal
        val otherInstance = PermissionManager(instanceSignal = instanceSignal)
        val userUuid = FakeData.user.uuid

        permissionService.start()
        otherInstance.start()

        awaitUntil { instanceSignal.subscriptions.value > 0 }

        val access = UserAccess(permissions = FakeData.user.permissions, deletedAt = null)
        assertNull(actual = permissionService.get(userUuid = userUuid))
        permissionService.set(userUuid = userUuid, access = access)
        assertEquals(expected = access, actual = permissionService.get(userUuid = userUuid))

        awaitUntil { otherInstance.get(userUuid = userUuid) != null }
        assertEquals(expected = access, actual = otherInstance.get(userUuid = userUuid))

        // A removal has to propagate as a removal: decoding it as non-null would fail and leave the other instance serving a stale entry.
        permissionService.set(userUuid = userUuid, access = null)
        awaitUntil { otherInstance.get(userUuid = userUuid) == null }
        permissionService.stop()
        otherInstance.stop()

        assertNull(actual = permissionService.get(userUuid = userUuid))
        assertNull(actual = otherInstance.get(userUuid = userUuid))
    }

    /** Verifies that starting an already started service does not open a second observation of the instance signal. */
    @Test
    fun startIsIdempotent() = runServerTest {
        val permissionService = dependency.get().permissionService
        val instanceSignal = dependency.get().instanceSignal as LocalSignal

        permissionService.start()
        permissionService.start()
        awaitUntil { instanceSignal.subscriptions.value > 0 }

        assertEquals(expected = 1, actual = instanceSignal.subscriptions.value)
        permissionService.stop()
        awaitUntil { instanceSignal.subscriptions.value == 0 }
    }

    /** Verifies that an instance that was never started keeps its own cache, but does not pick up updates set on other instances. */
    @Test
    fun unstartedInstanceIgnoresOtherInstances() = runServerTest {
        val permissionService = dependency.get().permissionService
        val otherInstance = PermissionManager(instanceSignal = dependency.get().instanceSignal)
        val userUuid = FakeData.user.uuid
        val access = UserAccess(permissions = FakeData.user.permissions, deletedAt = null)

        permissionService.set(userUuid = userUuid, access = access)
        assertEquals(expected = access, actual = permissionService.get(userUuid = userUuid))

        delay(timeMillis = 200)
        assertNull(actual = otherInstance.get(userUuid = userUuid))

        permissionService.set(userUuid = userUuid, access = null)
        assertNull(actual = permissionService.get(userUuid = userUuid))
    }

    /** Verifies that permission updates are relayed with the sending instance id, so the sender can skip its own echo. */
    @Test
    fun setTagsPayloadWithInstanceId() = runServerTest {
        val permissionService = dependency.get().permissionService
        val instanceSignal = dependency.get().instanceSignal as LocalSignal
        val envelopes = mutableListOf<String>()
        coroutineScope {
            val job = launch { instanceSignal.observe(channel = "permissions").collect { envelopes.add(element = it) } }
            awaitUntil { instanceSignal.subscriptions.value > 0 }

            permissionService.set(userUuid = FakeData.user.uuid, access = UserAccess(permissions = FakeData.user.permissions, deletedAt = null))
            awaitUntil { envelopes.isNotEmpty() }

            assertNotNull(actual = envelopes.single().substringBefore(delimiter = '|').toUuid())
            job.cancel()
        }
    }
}
