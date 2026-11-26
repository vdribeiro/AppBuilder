package com.app.builder.ui.navigation.timepunch

import com.app.builder.domain.TimePunch

/** Actions supported by the Time Punch Detail Screen. */
sealed interface TimePunchDetailScreenAction {
    /** Delete the current time punch. */
    data object Delete: TimePunchDetailScreenAction
    /** Restore the current deleted time punch. */
    data object Restore: TimePunchDetailScreenAction
}

/**
 * State of the Time Punch Detail Screen.
 *
 * @property timePunch Time punch being viewed, or `null` while it's still loading.
 * @property userName Name of the user that punched the clock, or `null` when it is unknown.
 */
data class TimePunchDetailScreenState(
    val timePunch: TimePunch? = null,
    val userName: String? = null,
)
