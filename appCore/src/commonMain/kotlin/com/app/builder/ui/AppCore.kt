package com.app.builder.ui

import androidx.compose.runtime.Composable
import com.app.builder.ui.devicelocation.ProvideDeviceLocationProvider

/**
 * Provides all of this module's providers.
 *
 * @param content Composable content.
 */
@Composable
fun AppCore(content: @Composable () -> Unit) {
    ProvideDeviceLocationProvider {
        content()
    }
}