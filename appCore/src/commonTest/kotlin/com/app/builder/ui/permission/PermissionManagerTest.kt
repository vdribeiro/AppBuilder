package com.app.builder.ui.permission

import kotlin.test.Test
import kotlin.test.assertNotNull
import com.app.builder.test.TestCase

class PermissionManagerTest: TestCase() {

    /** Verifies that [LocalPermissionManager] provides a permission manager. */
    @Test
    fun localPermission() = runUITest {
        var manager: PermissionManager? = null
        setUI {
            manager = LocalPermissionManager.current
        }
        assertNotNull(actual = manager)
    }
}
