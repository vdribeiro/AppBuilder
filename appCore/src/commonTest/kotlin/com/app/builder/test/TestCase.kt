package com.app.builder.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavBackStack
import com.app.builder.core.nfc.TagProvider
import com.app.builder.data.storage.CoreFile
import com.app.builder.data.translation.TranslationCache
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.push.PushProvider
import com.app.builder.ui.AppTheme
import com.app.builder.ui.lifecycle.OverridableLifecycleOwner
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Navigator
import com.app.builder.ui.navigation.Router

/** Abstract class for defining test cases. It provides a hermetic testing environment. */
abstract class TestCase: PlatformTestCase() {

    /** Navigation router. */
    protected val router: Router = Navigator(backStack = NavBackStack())

    /** Resets shared state so each test runs in a hermetic environment. */
    private suspend fun resetData() {
        PushProvider.reset()
        TagProvider.reset()
        DeepLink.reset()
        CoreFile.all.forEach { it.reset() }
        TranslationCache.set(translations = emptyMap())
        router.backStack.clear()
    }

    override suspend fun beforeTest() {
        resetData()
    }

    override suspend fun afterTest() {
        resetData()
    }

    /**
     * Render a Composable within the test harness.
     *
     * @param compositionValues The composition values to provide.
     * @param content The Composable to render.
     */
    protected fun ComposeUiTest.setUI(
        compositionValues: List<ProvidedValue<*>> = emptyList(),
        content: @Composable () -> Unit
    ) {
        setContent {
            val providers = remember {
                buildList {
                    addAll(elements = compositionValues)
                    add(element = LocalRouter provides router)
                    add(element = LocalLifecycleOwner provides OverridableLifecycleOwner())
                }.toTypedArray()
            }

            CompositionLocalProvider(values = providers) {
                AppTheme {
                    content()
                }
            }
        }
        waitForIdle()
    }
}
