package com.app.builder.test

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.v2.runComposeUiTest
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.MockLogger
import com.app.builder.core.telemetry.Telemetry

/** Class for defining test cases. It provides the shared hermetic testing environment by handling feature flags, configs, telemetry and coroutine dispatchers. */
abstract class SharedTestCase {

    init {
        // Snapshot FakeData flags and configs
        FakeData.clientFlags
        FakeData.clientConfigs
        FakeData.serverFlags
        FakeData.serverConfigs
    }

    /** Invoked before each test. */
    protected abstract suspend fun beforeTest()

    /** Invoked after each test. */
    protected abstract suspend fun afterTest()

    /**
     * Sets the dispatcher for coroutines.
     *
     * @param dispatcher The dispatcher to set.
     */
    private fun setDispatcher(dispatcher: CoroutineDispatcher) {
        Dispatcher.Main = dispatcher
        Dispatcher.Default = dispatcher
        Dispatcher.IO = dispatcher
    }

    /** Reset feature flags and configs. */
    private fun resetConfigs() {
        ServerFlags.reset()
        ServerConfigs.reset()
        ClientFlags.reset()
        ClientConfigs.reset()
    }

    /**
     * Sets up the test environment.
     *
     * @param dispatcher The dispatcher to set.
     */
    private suspend fun setup(dispatcher: CoroutineDispatcher) {
        setDispatcher(dispatcher = dispatcher)
        Dispatchers.setMain(dispatcher = dispatcher)
        Telemetry.engines.clear()
        Telemetry.engines.add(element = MockLogger())
        resetConfigs()
        beforeTest()
    }

    /**
     * Resets the test environment.
     *
     * @param scope The coroutine scope to cancel.
     */
    protected suspend fun reset(scope: CoroutineScope) {
        afterTest()
        resetConfigs()
        Telemetry.engines.clear()
        scope.cancel()
        setDispatcher(dispatcher = Dispatchers.Unconfined)
    }

    /**
     * Executes a unit test.
     * Prepares the environment by calling [setup], then executes the given [block]. Finally clears with [reset].
     *
     * @param timeout The maximum duration the test is allowed to run before failing.
     * @param block The block to execute.
     */
    protected fun runUnitTest(timeout: Duration = 60.seconds, block: suspend TestScope.(TestDispatcher) -> Unit) {
        runTest(timeout = timeout) {
            val testDispatcher = UnconfinedTestDispatcher(scheduler = testScheduler)
            val scope = backgroundScope
            setup(dispatcher = testDispatcher)
            try {
                block(testDispatcher)
                testScheduler.advanceUntilIdle()
            } finally {
                reset(scope = scope)
            }
        }
        Dispatchers.resetMain()
    }

    /**
     * Executes a UI test.
     * Prepares the environment by calling [setup], then executes the given [block]. Finally clears with [reset].
     *
     * @param timeout The maximum duration the test is allowed to run before failing.
     * @param block The block to execute.
     */
    protected fun runUITest(timeout: Duration = 60.seconds, block: suspend ComposeUiTest.(TestDispatcher) -> Unit) {
        runComposeUiTest(testTimeout = timeout) {
            val testDispatcher = UnconfinedTestDispatcher()
            val scope = CoroutineScope(context = testDispatcher)
            setup(dispatcher = testDispatcher)
            try {
                block(testDispatcher)
                waitForIdle()
            } finally {
                reset(scope = scope)
            }
        }
        Dispatchers.resetMain()
    }

    /**
     * Executes a unit test.
     * Prepares the environment by calling [setup], then executes the given [block]. Finally clears with [reset].
     *
     * @param timeout The maximum duration the test is allowed to run before failing.
     */
    protected fun runServerTest(timeout: Duration = 60.seconds, block: suspend ApplicationTestBuilder.(TestDispatcher) -> Unit) {
        testApplication {
            val testDispatcher = UnconfinedTestDispatcher()
            val scope = CoroutineScope(context = testDispatcher)
            setup(dispatcher = testDispatcher)
            try {
                withTimeout(timeout = timeout) { block(testDispatcher) }
            } finally {
                reset(scope = scope)
            }
        }
        Dispatchers.resetMain()
    }
}
