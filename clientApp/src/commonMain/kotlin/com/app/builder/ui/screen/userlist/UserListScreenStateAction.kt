package com.app.builder.ui.screen.userlist

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.app.builder.ui.component.list.UserItem

/** Actions that can be dispatched to a user list store. */
sealed interface UserListScreenAction {
    /**
     * Selects a user, navigating to its detail screen.
     *
     * @param userUuid UUID of the user that was selected.
     */
    data class SelectUser(val userUuid: String): UserListScreenAction
}

/**
 * State of the user list, holding the users to display.
 *
 * @property users Users currently displayed.
 */
data class UserListScreenState(
    val users: ImmutableList<UserItem> = persistentListOf(),
)
