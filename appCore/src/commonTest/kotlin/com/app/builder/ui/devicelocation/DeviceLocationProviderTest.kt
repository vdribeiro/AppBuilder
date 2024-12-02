package com.app.builder.ui.devicelocation

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.app.builder.core.devicelocation.DeviceLocationProvider
import com.app.builder.test.TestCase

class DeviceLocationProviderTest: TestCase() {

    /** Verifies that [ProvideDeviceLocationProvider] exposes a non-null [DeviceLocationProvider] through [LocalDeviceLocationProvider]. */
    @Test
    fun provideDeviceLocationProviderExposesAProvider() = runUITest {
        var provider: DeviceLocationProvider? = null

        setUI {
            ProvideDeviceLocationProvider {
                provider = LocalDeviceLocationProvider.current
            }
        }

        assertNotNull(actual = provider)
    }

    /** Verifies that the provided [DeviceLocationProvider] is the shared singleton instance. */
    @Test
    fun provideDeviceLocationProviderUsesTheSharedInstance() = runUITest {
        var provider: DeviceLocationProvider? = null

        setUI {
            ProvideDeviceLocationProvider {
                provider = LocalDeviceLocationProvider.current
            }
        }

        assertSame(expected = DeviceLocationProvider.instance, actual = provider)
    }

    /** Verifies that the provided [DeviceLocationProvider] is stable across recompositions. */
    @Test
    fun provideDeviceLocationProviderIsStableAcrossRecompositions() = runUITest {
        val providers = mutableListOf<DeviceLocationProvider>()

        setUI {
            ProvideDeviceLocationProvider {
                val counter = remember { mutableIntStateOf(value = 0) }
                providers.add(element = LocalDeviceLocationProvider.current)
                LaunchedEffect(key1 = Unit) { counter.value = 1 }
            }
        }
        waitForIdle()

        assertNotNull(actual = providers.firstOrNull())
        assertSame(expected = providers.first(), actual = providers.last())
    }
}
