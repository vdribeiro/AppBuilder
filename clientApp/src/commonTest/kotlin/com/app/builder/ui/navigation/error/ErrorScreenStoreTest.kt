package com.app.builder.ui.navigation.error

import kotlin.test.Test
import kotlinx.coroutines.flow.first
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Console
import com.app.builder.test.TestCase

class ErrorScreenStoreTest: TestCase() {

    /** Verifies that the state holds the console logs and drops them when the console client flag turns off. */
    @Test
    fun logsFollowTheConsole() = runUnitTest {
        val store = ErrorScreenStore(state = ErrorScreenState())
        Console.feedback(message = "store_marker")

        store.stateFlow.first { it.logs?.contains(other = "FEEDBACK: store_marker") == true }

        ClientFlags.set { it.copy(console = false) }

        store.stateFlow.first { it.logs == null }
    }
}
