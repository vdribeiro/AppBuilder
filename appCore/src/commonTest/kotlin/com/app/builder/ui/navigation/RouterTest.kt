package com.app.builder.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey
import com.app.builder.test.TestCase

class RouterTest: TestCase() {

    /** Verifies that navigating back removes the last screen from the back stack. */
    @Test
    fun back() = runUnitTest {
        assertTrue(actual = router.backStack.isEmpty())

        router.navigate(screen = FakeScreen.Main)
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())

        router.back()
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List), actual = router.backStack.toList())

        router.back()
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.ADD] appends the screen to the back stack. */
    @Test
    fun add() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List, option = Router.NavOption.ADD)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List, option = Router.NavOption.ADD)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.List), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.CLEAR] clears the back stack down to the new screen. */
    @Test
    fun clear() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)
        router.navigate(screen = FakeScreen.List)
        router.navigate(screen = FakeScreen.Detail(uuid = "1"))
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.Detail(uuid = "1")), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List, option = Router.NavOption.CLEAR)
        assertEquals(expected = listOf(FakeScreen.List), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.REPLACE_FIRST] replaces the entire back stack with the new screen. */
    @Test
    fun replaceFirst() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)
        router.navigate(screen = FakeScreen.List)
        router.navigate(screen = FakeScreen.Main)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.Main), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.Main, option = Router.NavOption.REPLACE_FIRST)
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.REPLACE_LAST] replaces only the top of the back stack. */
    @Test
    fun replaceLast() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)
        router.navigate(screen = FakeScreen.List)
        router.navigate(screen = FakeScreen.Detail(uuid = "1"))
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.Detail(uuid = "1")), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.Detail(uuid = "2"), option = Router.NavOption.REPLACE_LAST)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.Detail(uuid = "2")), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List, option = Router.NavOption.REPLACE_LAST)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.SWAP] moves the screen to the top, deduplicating it if already present. */
    @Test
    fun swap() = runUnitTest {
        router.navigate(screen = FakeScreen.Main, option = Router.NavOption.SWAP)
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.Main, option = Router.NavOption.SWAP)
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List, option = Router.NavOption.SWAP)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.Detail(uuid = "1"), option = Router.NavOption.SWAP)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.Detail(uuid = "1")), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.Main, option = Router.NavOption.SWAP)
        assertEquals(expected = listOf(FakeScreen.List, FakeScreen.Detail(uuid = "1"), FakeScreen.Main), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.POP] pops the back stack until the target screen or its first occurrence. */
    @Test
    fun pop() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)
        router.navigate(screen = FakeScreen.List)
        router.navigate(screen = FakeScreen.Detail(uuid = "1"))
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.Detail(uuid = "1")), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List, option = Router.NavOption.POP)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List)
        router.navigate(screen = FakeScreen.Main)
        router.navigate(screen = FakeScreen.List, option = Router.NavOption.POP)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.List), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.IGNORE] skips navigation when the screen is already on top. */
    @Test
    fun ignore() = runUnitTest {
        router.navigate(screen = FakeScreen.Main, option = Router.NavOption.IGNORE)
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.Main, option = Router.NavOption.IGNORE)
        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())

        router.navigate(screen = FakeScreen.List, option = Router.NavOption.IGNORE)
        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List), actual = router.backStack.toList())
    }

    /** Verifies that [NoOpRouter] ignores all navigation and keeps the back stack empty. */
    @Test
    fun noOpRouter() = runUnitTest {
        val noOpRouter = NoOpRouter()
        noOpRouter.navigate(screen = FakeScreen.Main)
        assertTrue(actual = noOpRouter.backStack.isEmpty())
        noOpRouter.back()
        assertTrue(actual = noOpRouter.backStack.isEmpty())
    }

    /** Verifies that [Router.NavOption.REPLACE_FIRST] simply appends the screen when it is absent from the back stack. */
    @Test
    fun replaceFirstWhenScreenAbsentJustAppends() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)
        router.navigate(screen = FakeScreen.List)

        router.navigate(screen = FakeScreen.Detail(uuid = "1"), option = Router.NavOption.REPLACE_FIRST)

        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List, FakeScreen.Detail(uuid = "1")), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.REPLACE_LAST] simply appends the screen when it is absent from the back stack. */
    @Test
    fun replaceLastWhenScreenAbsentJustAppends() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)

        router.navigate(screen = FakeScreen.Detail(uuid = "1"), option = Router.NavOption.REPLACE_LAST)

        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.Detail(uuid = "1")), actual = router.backStack.toList())
    }

    /** Verifies that [Router.NavOption.POP] leaves the back stack unchanged when the target screen is absent. */
    @Test
    fun popWhenScreenAbsentLeavesStackUnchanged() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)
        router.navigate(screen = FakeScreen.List)

        router.navigate(screen = FakeScreen.Detail(uuid = "1"), option = Router.NavOption.POP)

        assertEquals(expected = listOf(FakeScreen.Main, FakeScreen.List), actual = router.backStack.toList())
    }

    /** Verifies that navigating back on an empty back stack is a no-op. */
    @Test
    fun backOnEmptyStackDoesNothing() = runUnitTest {
        assertTrue(actual = router.backStack.isEmpty())

        router.back()

        assertTrue(actual = router.backStack.isEmpty())
    }

    /** Verifies that navigating back on a single-screen back stack keeps that screen in place. */
    @Test
    fun backOnSingleScreenStackDoesNothing() = runUnitTest {
        router.navigate(screen = FakeScreen.Main)

        router.back()

        assertEquals(expected = listOf(FakeScreen.Main), actual = router.backStack.toList())
    }

    /** A fake screen fixture used to exercise [Router] navigation. */
    @Serializable
    sealed interface FakeScreen: NavKey {
        /** A fake screen fixture representing the main screen. */
        @Serializable
        data object Main: FakeScreen
        /** A fake screen fixture representing a list screen. */
        @Serializable
        data object List: FakeScreen
        /**
         * A fake screen fixture representing a detail screen.
         *
         * @property uuid The fixture's identifier.
         */
        @Serializable
        data class Detail(val uuid: String): FakeScreen
    }
}
