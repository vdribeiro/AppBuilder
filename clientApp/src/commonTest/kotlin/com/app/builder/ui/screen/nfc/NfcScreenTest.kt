package com.app.builder.ui.screen.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.app.builder.core.nfc.NfcController
import com.app.builder.core.nfc.NfcRecord
import com.app.builder.domain.EntityType
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.LocalNfcController
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.actionbar.ActionBarStore
import com.app.builder.ui.component.navigation.NavigationRoute
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.navigation.NavigationStore

/**
 * Reports the given availability and records what it is asked to write, without reaching any platform NFC API.
 *
 * @property available Whether the controller reports an NFC adapter.
 */
private class FakeNfcController(override val available: Boolean): NfcController() {
    /** Records handed to every write session, in order. */
    val written = mutableListOf<List<NfcRecord>>()

    override fun platformWrite(records: List<NfcRecord>) {
        written += records
    }
}

class NfcScreenTest: TestCase() {

    /**
     * Renders the NFC screen backed by the given controller.
     *
     * @param nfcController The controller the screen drives.
     */
    private suspend fun ComposeUiTest.setUpScreen(nfcController: NfcController) {
        val authenticationUseCases = dependency.get()
            .userGraph
            .useCases
            .authenticationUseCases

        val actionBarStore = ActionBarStore(state = ActionBarState(), router = router, authenticationUseCases = authenticationUseCases)
        val navigationStore = NavigationStore(state = NavigationState(selected = NavigationRoute.NFC), router = router, authenticationUseCases = authenticationUseCases)

        setUI {
            CompositionLocalProvider(LocalNfcController provides nfcController) {
                NfcScreen(actionBarStore = actionBarStore, navigationStore = navigationStore)
            }
        }
    }

    /** Verifies that without an NFC adapter neither scanning nor writing can be started. */
    @Test
    fun unavailable() = runUITest {
        setUpScreen(nfcController = FakeNfcController(available = false))

        onNodeWithText(text = "false").assertIsDisplayed()
        onNodeWithText(text = "Start Scan").assertIsNotEnabled()
        onNodeWithText(text = "Stop").assertIsNotEnabled()
        onNodeWithText(text = "Write Tag").assertIsNotEnabled()
    }

    /** Verifies that the scan controls follow the controller from idle to scanning and back. */
    @Test
    fun scan() = runUITest {
        val nfcController = FakeNfcController(available = true)
        setUpScreen(nfcController = nfcController)

        onNodeWithText(text = "Start Scan").assertIsEnabled()
        onNodeWithText(text = "Stop").assertIsNotEnabled()

        onNodeWithText(text = "Start Scan").performClick()
        waitUntil { nfcController.state.value == NfcController.State.Scanning }

        onNodeWithText(text = "Start Scan").assertIsNotEnabled()
        onNodeWithText(text = "Stop").assertIsEnabled().performClick()
        waitUntil { nfcController.state.value == NfcController.State.Idle }

        onNodeWithText(text = "Start Scan").assertIsEnabled()
    }

    /** Verifies that a malformed entity uuid blocks writing, and that a valid one writes an open record for it until the write is canceled. */
    @Test
    fun write() = runUITest {
        val nfcController = FakeNfcController(available = true)
        setUpScreen(nfcController = nfcController)

        onNodeWithText(text = "Write Tag").assertIsEnabled()

        onNode(matcher = hasSetTextAction()).performTextReplacement(text = "invalid")
        onNodeWithText(text = "Write Tag").assertIsNotEnabled()

        onNode(matcher = hasSetTextAction()).performTextReplacement(text = FakeData.task.uuid.toString())
        onNodeWithText(text = "Write Tag").assertIsEnabled().performClick()
        waitUntil { nfcController.state.value == NfcController.State.Writing }

        assertEquals(expected = listOf(listOf<NfcRecord>(NfcRecord.Open(entityType = EntityType.TASK, entityUuid = FakeData.task.uuid))), actual = nfcController.written)

        onNodeWithText(text = "Cancel Write").assertIsDisplayed().performClick()
        waitUntil { nfcController.state.value == NfcController.State.Idle }

        onNodeWithText(text = "Cancel Write").assertDoesNotExist()
        assertTrue(actual = nfcController.written.size == 1)
    }
}
