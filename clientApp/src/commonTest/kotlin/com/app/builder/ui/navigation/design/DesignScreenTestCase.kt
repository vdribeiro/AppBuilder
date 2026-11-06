package com.app.builder.ui.navigation.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import com.app.builder.data.theme.CustomTheme
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase
import com.app.builder.ui.component.session.SessionState
import com.app.builder.ui.store.Store

/** Base for the design screen tests, holding the stores and rendering helper they share. */
abstract class DesignScreenTestCase: TestCase() {

    /** Store recording every action the screen sends, without acting on them. */
    protected class RecordingStore: Store<Unit, DesignScreenAction>(initialState = Unit) {
        /** The actions received so far, in order. */
        val actions = mutableListOf<DesignScreenAction>()
        /** The theme of the last save received, or `null` when none was. */
        val savedTheme: CustomTheme? get() = actions.filterIsInstance<DesignScreenAction.SaveTheme>().lastOrNull()?.customTheme

        override fun reducer(state: Unit, action: DesignScreenAction) {
            actions.add(element = action)
        }
    }

    /** Session store holding a signed in user, whose account opens the design routes. */
    protected val sessionStore = Store<SessionState, Unit>(initialState = SessionState(loaded = true, user = FakeData.user, account = FakeData.account))

    /**
     * Renders showcase content with the test clock held still.
     * The showcases render components that animate forever, like a loading button or an indeterminate progress indicator, which never let the composition go idle if the clock is left to advance on its own.
     *
     * @param content The Composable to render.
     */
    protected fun ComposeUiTest.setShowcase(content: @Composable () -> Unit) {
        mainClock.autoAdvance = false
        setUI(content = content)
    }
}
