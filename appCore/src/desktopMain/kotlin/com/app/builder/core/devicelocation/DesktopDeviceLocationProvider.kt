package com.app.builder.core.devicelocation

import com.app.builder.test.ExcludeFromTesting

/** Desktop [DeviceLocationProvider] is a no-op. */
@ExcludeFromTesting
internal class DesktopDeviceLocationProvider: DeviceLocationProvider()

internal actual fun createDeviceLocationProvider(): DeviceLocationProvider = DesktopDeviceLocationProvider()
