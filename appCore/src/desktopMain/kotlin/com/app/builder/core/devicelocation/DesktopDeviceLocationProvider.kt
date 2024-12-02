package com.app.builder.core.devicelocation

/** Desktop [DeviceLocationProvider] is a no-op. */
internal class DesktopDeviceLocationProvider: DeviceLocationProvider()

internal actual fun createDeviceLocationProvider(): DeviceLocationProvider = DesktopDeviceLocationProvider()
