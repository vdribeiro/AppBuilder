package com.app.builder.core.nfc

/** Web [NfcController] is a no-op. */
internal class WebNfcController: NfcController()

internal actual fun createNfcController(): NfcController = WebNfcController()
