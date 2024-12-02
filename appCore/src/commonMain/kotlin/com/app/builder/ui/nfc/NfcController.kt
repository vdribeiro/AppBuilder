package com.app.builder.ui.nfc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.app.builder.core.nfc.NfcController

/**
 * Creates and remembers a [NfcController] reader/writer.
 *
 * @return [NfcController] instance.
 */
@Composable
fun rememberNfcController(): NfcController = remember { NfcController.create() }.also { RegisterNfcControllerLifecycle(nfcController = it) }

/**
 * Registers as the host for NFC scans while it stays composed, attaching/detaching the platform's active [NfcController] session as it enters/leaves composition and foreground/background.
 *
 * @param nfcController to register the lifecycle.
 */
@Composable
internal expect fun RegisterNfcControllerLifecycle(nfcController: NfcController)
