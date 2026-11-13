package com.app.builder.domain.gateway.account

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.core.telemetry.TelemetryEngine
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class AccountPermissionTest: TestCase() {

    /** An account allowed to read equipments but not to write them. */
    private val readOnlyAccount: Account = FakeData.account.copy(permissions = mapOf(EntityType.EQUIPMENT to Permission.READ))

    /** Verifies a granted permission returns no failure and reports nothing through telemetry. */
    @Test
    fun returnsNullWhenPermissionIsGranted() = runUnitTest {
        val errors = collectErrors()

        assertNull(actual = readOnlyAccount.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT, permission = Permission.READ))
        assertTrue(actual = errors.isEmpty())
    }

    /** Verifies a denied permission returns the failure and reports it through telemetry under the given tag. */
    @Test
    fun returnsFailureAndReportsWhenPermissionIsDenied() = runUnitTest {
        val errors = collectErrors()

        val throwable = assertNotNull(actual = readOnlyAccount.validatePermission(tag = TAG, entityType = EntityType.EQUIPMENT, permission = Permission.WRITE))

        assertEquals(expected = "User does not have WRITE permission for EQUIPMENT", actual = throwable.message)
        val (message, reported) = errors.single()
        assertEquals(expected = "Invalid permissions", actual = message)
        assertSame(expected = throwable, actual = reported)
    }

    /** Verifies an entity type missing from the account permissions is denied. */
    @Test
    fun returnsFailureWhenEntityTypeIsNotListed() = runUnitTest {
        val errors = collectErrors()

        assertNotNull(actual = readOnlyAccount.validatePermission(tag = TAG, entityType = EntityType.BUILDING, permission = Permission.READ))
        assertEquals(expected = 1, actual = errors.size)
    }

    /**
     * Registers a telemetry engine that collects the errors reported under [TAG].
     *
     * @return The collected error messages paired with their throwables.
     */
    private fun collectErrors(): List<Pair<String, Throwable?>> {
        val errors = mutableListOf<Pair<String, Throwable?>>()
        Telemetry.engines += object: TelemetryEngine {
            override fun info(tag: String, message: String) {}
            override fun error(tag: String, message: String, throwable: Throwable?) {
                if (tag == TAG) errors.add(element = message to throwable)
            }

            override fun feedback(message: String) {}
        }
        return errors
    }

    private companion object {
        private const val TAG = "AccountPermissionTest"
    }
}
