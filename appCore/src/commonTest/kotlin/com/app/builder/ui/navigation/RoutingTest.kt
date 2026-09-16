package com.app.builder.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.platform
import com.app.builder.test.TestCase

class RoutingTest: TestCase() {

    /** Verifies that the screen matching the current back stack entry is rendered. */
    @Test
    fun rendersScreen() = runUITest {
        assertTrue(actual = router.backStack.isEmpty())

        router.navigate(screen = FakeScreen.First)
        waitForIdle()

        setUI { Navigation(entryProviderScope = { registerFakeScreens() }) }

        onNodeWithText(text = "First Screen").assertExists()
        onNodeWithText(text = "Second Screen").assertDoesNotExist()

        router.navigate(screen = FakeScreen.Second)
        waitForIdle()

        onNodeWithText(text = "First Screen").assertDoesNotExist()
        onNodeWithText(text = "Second Screen").assertExists()
    }

    /** Falls back to the fallback screen when the requested screen is unregistered. */
    @Test
    fun unknownScreenFallsBackToTheFallbackScreen() = runUITest {
        router.navigate(screen = FakeScreen.Unregistered)
        waitForIdle()

        setUI {
            Navigation(
                fallbackScreen = FakeScreen.First,
                entryProviderScope = { registerFakeScreens() }
            )
        }
        waitForIdle()

        assertEquals(expected = listOf(FakeScreen.First), actual = router.backStack.toList())
        onNodeWithText(text = "First Screen").assertExists()
    }

    /** Verifies that [rememberRouter] starts the back stack from the given initial stack. */
    @Test
    fun rememberRouterStartsOnTheInitialStack() = runUITest {
        var remembered: Router? = null

        setUI {
            remembered = rememberRouter<FakeScreen>(initialStack = listOf(FakeScreen.Second))
        }
        waitForIdle()

        val router = assertNotNull(actual = remembered)
        assertEquals(expected = listOf(FakeScreen.Second), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.First)
        assertEquals(expected = listOf(FakeScreen.Second, FakeScreen.First), actual = router.backStack.toList())
        router.back()
        assertEquals(expected = listOf(FakeScreen.Second), actual = router.backStack.toList())
    }

    /**
     * Throws when the requested screen is unregistered and no fallback screen was provided.
     * The assertion wraps the whole UI test because the throw happens inside composition, which leaves the Compose test harness unable to reach idle again once the exception is caught within it.
     * Web is skipped because its UI test harness completes asynchronously, so the throw only surfaces after the assertion has already returned.
     */
    @Test
    fun unknownScreenWithoutFallbackThrows() {
        when (platform.os) {
            OS.Web -> return
            OS.Android, OS.Ios, OS.Windows, OS.Mac, OS.Linux, OS.Unknown -> Unit
        }
        assertFailsWith<IllegalStateException> {
            runUITest {
                router.navigate(screen = FakeScreen.Unregistered)
                waitForIdle()

                setUI { Navigation(entryProviderScope = { registerFakeScreens() }) }
            }
        }
    }

    /** Registers the fake screens used by these tests as navigation entries. */
    private fun EntryProviderScope<NavKey>.registerFakeScreens() {
        entry<FakeScreen.First> { BasicText(text = "First Screen") }
        entry<FakeScreen.Second> { BasicText(text = "Second Screen") }
    }

    /** A fake screen fixture used to exercise routing. */
    @Serializable
    sealed interface FakeScreen: NavKey {
        /** A fake screen fixture representing the first registered screen. */
        @Serializable
        data object First: FakeScreen
        /** A fake screen fixture representing the second registered screen. */
        @Serializable
        data object Second: FakeScreen
        /** A fake screen fixture representing a screen with no registered entry. */
        @Serializable
        data object Unregistered: FakeScreen
    }
}
