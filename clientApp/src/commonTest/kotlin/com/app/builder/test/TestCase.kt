package com.app.builder.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavBackStack
import com.app.builder.Application.reset
import com.app.builder.Dependency
import com.app.builder.core.flow.LazyData
import com.app.builder.data.database.createMockSqlDriver
import com.app.builder.data.http.createMockHttpEngine
import com.app.builder.ui.AppTheme
import com.app.builder.ui.lifecycle.OverridableLifecycleOwner
import com.app.builder.ui.navigation.LocalRouter
import com.app.builder.ui.navigation.Navigator
import com.app.builder.ui.navigation.Router

/** Abstract class for defining test cases. It provides a hermetic testing environment. */
abstract class TestCase: PlatformTestCase() {

    /** Dependency index for test cases with in memory Database and Mock Http Engine. */
    protected val dependency: LazyData<Dependency.AppGraph> = LazyData {
        Dependency.AppGraph(
            sqlDriver = createMockSqlDriver(),
            httpClientEngine = createMockHttpEngine()
        )
    }

    /** Navigation router. */
    protected val router: Router = Navigator(backStack = NavBackStack())

    override suspend fun beforeTest() {
        resetData()
    }

    override suspend fun afterTest() {
        resetData()
    }

    /** Resets push registration, provider state, dependency data, translation cache, and navigation back stack between tests. */
    private suspend fun resetData() {
        val dependency = dependency.get()
        dependency.reset()
        router.backStack.clear()
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
                compositionValues + listOf(
                    LocalRouter provides router,
                    LocalLifecycleOwner provides OverridableLifecycleOwner()
                )
            }.toTypedArray()

            CompositionLocalProvider(values = providers) {
                AppTheme {
                    content()
                }
            }
        }
        waitForIdle()
    }
}
