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

        assertNull(actual = permissionService.get(userUuid = userUuid))
        permissionService.set(userUuid = userUuid, permissions = FakeData.user.permissions)
        assertEquals(expected = FakeData.user.permissions, actual = permissionService.get(userUuid = userUuid))

        awaitUntil { otherInstance.get(userUuid = userUuid) != null }
        assertEquals(expected = FakeData.user.permissions, actual = otherInstance.get(userUuid = userUuid))
        permissionService.stop()
        otherInstance.stop()

        assertNull(actual = permissionService.get(userUuid = userUuid))
        assertNull(actual = otherInstance.get(userUuid = userUuid))
    }
}
