package com.app.builder.ui.navigation.nfc

/**
 * State for the NFC screen.
 *
 * @property writable Whether the current user can write NFC tags.
 */
data class NfcScreenState(
    val writable: Boolean = false,
)
