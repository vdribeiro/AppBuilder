package com.app.builder.ui.devicelocation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.app.builder.core.devicelocation.DeviceLocationProvider
import com.app.builder.ui.lifecycle.Register

/** CompositionLocal providing the current [DeviceLocationProvider]. */
val LocalDeviceLocationProvider: ProvidableCompositionLocal<DeviceLocationProvider> = staticCompositionLocalOf { DeviceLocationProvider() }

/**
 * Provides the [DeviceLocationProvider] singleton, with a registered lifecycle, to [content] via [LocalDeviceLocationProvider].
 *
 * @param content Composable content that can read [LocalDeviceLocationProvider].
 */
@Composable
fun ProvideDeviceLocationProvider(content: @Composable () -> Unit) {
    val deviceLocationProvider = rememberDeviceLocationProvider()
    CompositionLocalProvider(LocalDeviceLocationProvider provides deviceLocationProvider, content = content)
}

/**
 * Remembers the [DeviceLocationProvider] with a lifecycle.
 *
 * @return [DeviceLocationProvider] instance.
 */
@Composable
private fun rememberDeviceLocationProvider(): DeviceLocationProvider = remember { DeviceLocationProvider.instance }.also { RegisterDeviceLocationLifecycle(deviceLocationProvider = it) }

/** Registers a lifecycle callback for the location provider to pause when the app is in the background and resume when the app is in the foreground. */
@Composable
private fun RegisterDeviceLocationLifecycle(deviceLocationProvider: DeviceLocationProvider) {
    Register(
        deviceLocationProvider,
        onBackground = { deviceLocationProvider.pauseUpdate() },
        onForeground = { deviceLocationProvider.resumeUpdate() },
        onDispose = { deviceLocationProvider.stopUpdate() }
    )
}