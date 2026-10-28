package com.app.builder.ui

import kotlin.test.Test
import kotlin.test.assertSame
import com.app.builder.core.devicelocation.DeviceLocationProvider
import com.app.builder.test.TestCase
import com.app.builder.ui.devicelocation.LocalDeviceLocationProvider

class AppCoreTest: TestCase() {

    /** Verifies that [AppCore] renders its content with the shared [DeviceLocationProvider] provided. */
    @Test
    fun appCoreProvidesTheSharedDeviceLocationProviderToItsContent() = runUITest {
        var provided: DeviceLocationProvider? = null

        setUI {
            AppCore {
                provided = LocalDeviceLocationProvider.current
            }
        }

        assertSame(expected = DeviceLocationProvider.instance, actual = provided)
    }
}
