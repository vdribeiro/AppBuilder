package com.app.builder.ui.navigation.error

/**
 * State for the error screen.
 *
 * @property error true if the user was redirected to this screen because of an error, false if the user actively navigated to this screen.
 * @property logs The console logs, or `null` when there are none.
 */
data class ErrorScreenState(
    val error: Boolean = false,
    val logs: String? = null,
)
