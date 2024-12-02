package com.app.builder.core.nfc

/** Desktop [NfcController] is a no-op. */
internal class DesktopNfcController: NfcController()

internal actual fun createNfcController(): NfcController = DesktopNfcController()
