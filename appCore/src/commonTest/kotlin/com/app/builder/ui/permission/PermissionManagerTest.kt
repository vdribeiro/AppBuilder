package com.app.builder.ui.permission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.test.TestCase

class PermissionManagerTest: TestCase() {

    /**
     * A [PermissionManager] fixture that records every check and request, answering requests once [gate] completes.
     *
     * @property granted Whether the permission is already granted.
     * @property gate Completes with the result of every platform request.
     */
    private class RecordingPermissionManager(
        private val granted: Boolean = false,
        var gate: CompletableDeferred<Boolean> = CompletableDeferred(value = true)
    ): PermissionManager() {
        /** The permissions checked so far, in order. */
        val checks = mutableListOf<Permission>()
        /** The permissions requested from the platform so far, in order. */
        val requests = mutableListOf<Permission>()

        override suspend fun hasPermission(permission: Permission): Boolean {
            checks.add(element = permission)
            return granted
        }

        override suspend fun requestPermission(permission: Permission): Boolean {
            requests.add(element = permission)
            return gate.await()
        }
    }

    /** Verifies that [LocalPermissionManager] provides a permission manager. */
    @Test
    fun localPermission() = runUITest {
        var manager: PermissionManager? = null
        setUI {
            manager = LocalPermissionManager.current
        }
        assertNotNull(actual = manager)
    }

    /** Verifies that [rememberPermissionManager] provides the platform's permission manager. */
    @Test
    fun rememberPermissionManagerProvidesAManager() = runUITest {
        var manager: PermissionManager? = null
        setUI {
            manager = rememberPermissionManager()
        }
        assertNotNull(actual = manager)
    }

    /** Verifies that the base [PermissionManager] reports every permission as missing and refuses to grant it, since its default request is refused. */
    @Test
    fun basePermissionManagerRefusesEveryPermission() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val manager = PermissionManager()

        Permission.entries.forEach {
            assertFalse(actual = manager.hasPermission(permission = it))
            assertFalse(actual = manager.grantPermission(permission = it))
        }
    }

    /** Verifies that a permission whose feature flag is disabled is refused without checking or requesting it. */
    @Test
    fun grantPermissionRefusesPermissionsWhoseFeatureIsDisabled() = runUnitTest {
        ClientFlags.set { it.copy(notifications = false, camera = false, locationCapture = false) }
        val manager = RecordingPermissionManager(granted = true)

        Permission.entries.forEach { assertFalse(actual = manager.grantPermission(permission = it)) }

        assertTrue(actual = manager.checks.isEmpty())
        assertTrue(actual = manager.requests.isEmpty())
    }

    /** Verifies that an already granted permission is reported as granted without requesting it again. */
    @Test
    fun grantPermissionSkipsTheRequestWhenAlreadyGranted() = runUnitTest {
        val manager = RecordingPermissionManager(granted = true)

        assertTrue(actual = manager.grantPermission(permission = Permission.CAMERA))

        assertEquals(expected = listOf(Permission.CAMERA), actual = manager.checks)
        assertTrue(actual = manager.requests.isEmpty())
    }

    /** Verifies that a missing permission is requested and the request's answer is returned. */
    @Test
    fun grantPermissionReturnsTheRequestResult() = runUnitTest {
        val manager = RecordingPermissionManager(gate = CompletableDeferred(value = false))

        assertFalse(actual = manager.grantPermission(permission = Permission.NOTIFICATIONS))

        assertEquals(expected = listOf(Permission.NOTIFICATIONS), actual = manager.requests)
    }

    /** Verifies that concurrent grants of the same permission share a single in-flight platform request, and a later grant starts a new one. */
    @Test
    fun concurrentGrantsShareTheInFlightRequest() = runUnitTest {
        val gate = CompletableDeferred<Boolean>()
        val manager = RecordingPermissionManager(gate = gate)

        val first = async(context = Dispatcher.Default) { manager.grantPermission(permission = Permission.CAMERA) }
        val second = async(context = Dispatcher.Default) { manager.grantPermission(permission = Permission.CAMERA) }
        testScheduler.runCurrent()
        assertEquals(expected = listOf(Permission.CAMERA), actual = manager.requests)

        gate.complete(value = true)
        assertTrue(actual = first.await())
        assertTrue(actual = second.await())

        manager.gate = CompletableDeferred(value = false)
        assertFalse(actual = manager.grantPermission(permission = Permission.CAMERA))
        assertEquals(expected = listOf(Permission.CAMERA, Permission.CAMERA), actual = manager.requests)
    }

    /** Verifies that in-flight requests are tracked per permission, so different permissions are requested independently. */
    @Test
    fun differentPermissionsAreRequestedIndependently() = runUnitTest {
        ClientFlags.set { it.copy(locationCapture = true) }
        val gate = CompletableDeferred<Boolean>()
        val manager = RecordingPermissionManager(gate = gate)

        val camera = async(context = Dispatcher.Default) { manager.grantPermission(permission = Permission.CAMERA) }
        val location = async(context = Dispatcher.Default) { manager.grantPermission(permission = Permission.LOCATION) }
        testScheduler.runCurrent()
        assertEquals(expected = listOf(Permission.CAMERA, Permission.LOCATION), actual = manager.requests)

        gate.complete(value = true)
        assertTrue(actual = camera.await())
        assertTrue(actual = location.await())
    }
}
