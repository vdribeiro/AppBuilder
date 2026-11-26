package com.app.builder.ui.navigation.timepunch

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import com.app.builder.domain.TimePunch

/** Actions supported by the Time Punch Detail Screen. */
sealed interface TimePunchDetailScreenAction {
    /**
     * Updates the time at which the user punched the clock.
     *
     * @param time The new time value.
     */
    data class ChangeTime(val time: Instant): TimePunchDetailScreenAction
    /**
     * Updates the user that punched the clock.
     *
     * @param userUuid The unique identifier of the new user.
     */
    data class ChangeUser(val userUuid: Uuid): TimePunchDetailScreenAction
    /** Start editing the current time punch. */
    data object Edit: TimePunchDetailScreenAction
    /** Save the edited time punch and stop editing it. */
    data object Save: TimePunchDetailScreenAction
    /** Discard the edits, then reload the stored time punch and stop editing it, or leave the screen when the time punch is new. */
    data object Cancel: TimePunchDetailScreenAction
    /** Delete the current time punch. */
    data object Delete: TimePunchDetailScreenAction
    /** Restore the current deleted time punch. */
    data object Restore: TimePunchDetailScreenAction
}

/**
 * State of the Time Punch Detail Screen.
 *
 * @property timePunch Time punch being viewed or edited, or `null` while it's still loading.
 * @property editing Whether the time punch is being edited, which stops it from following the stored time punch.
 * @property new Whether the time punch is new and not stored yet.
 * @property userNames Names of the known users by their unique identifier.
 */
data class TimePunchDetailScreenState(
    val timePunch: TimePunch? = null,
    val editing: Boolean = false,
    val new: Boolean = false,
    val userNames: ImmutableMap<Uuid, String> = persistentMapOf(),
)
