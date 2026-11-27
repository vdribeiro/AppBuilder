package com.app.builder.ui.navigation.timepunch

import kotlin.uuid.Uuid
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.TimePunchItem

/** Actions that can be dispatched to a time punch list store. */
sealed interface TimePunchListScreenAction {
    /**
     * Selects a time punch, navigating to its detail screen.
     *
     * @param timePunchUuid UUID of the time punch that was selected.
     */
    data class SelectTimePunch(val timePunchUuid: String): TimePunchListScreenAction
}

/**
 * State of the time punch list.
 *
 * @property timePunches Time punches currently displayed.
 * @property userUuid UUID of the current user, or `null` when no user is signed in.
 */
data class TimePunchListScreenState(
    val timePunches: ImmutableList<TimePunchItem> = persistentListOf(),
    val userUuid: Uuid? = null,
)
