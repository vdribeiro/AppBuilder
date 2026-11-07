package com.app.builder.ui.component.session

import com.app.builder.domain.Account
import com.app.builder.domain.User

/**
 * State of the current session, shared by every screen of a navigation host.
 *
 * @property loaded Whether the current user and account were resolved at least once.
 * @property user The current user, or `null` when no user is signed in.
 * @property account The current user's account, or `null` when no user is signed in.
 * @property synced Whether no background job is pending or running.
 * @property clockTrusted Whether the device clock can be trusted.
 */
data class SessionState(
    val loaded: Boolean = false,
    val user: User? = null,
    val account: Account? = null,
    val synced: Boolean = true,
    val clockTrusted: Boolean = true,
)
