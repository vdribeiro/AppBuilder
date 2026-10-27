package com.app.builder.core.nfc

import com.app.builder.test.ExcludeFromTesting

/** Desktop [NfcController] is a no-op. */
@ExcludeFromTesting
internal class DesktopNfcController: NfcController()

internal actual fun createNfcController(): NfcController = DesktopNfcController()
