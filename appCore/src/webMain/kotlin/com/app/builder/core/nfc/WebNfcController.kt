package com.app.builder.core.nfc

import com.app.builder.test.ExcludeFromTesting

/** Web [NfcController] is a no-op. */
@ExcludeFromTesting
internal class WebNfcController: NfcController()

internal actual fun createNfcController(): NfcController = WebNfcController()
