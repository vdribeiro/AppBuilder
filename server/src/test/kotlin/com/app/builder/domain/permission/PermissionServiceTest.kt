package com.app.builder.domain.permission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
}
