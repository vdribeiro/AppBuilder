package com.app.builder.ui.nfc

import kotlin.test.Test
import kotlin.test.assertNotNull
import com.app.builder.core.nfc.NfcController
import com.app.builder.test.TestCase

class NfcControllerTest: TestCase() {

    /** Verifies that [rememberNfcController] provides a non-null [NfcController]. */
    @Test
    fun nfcReader() = runUITest {
        var reader: NfcController? = null
        setUI {
            reader = rememberNfcController()
        }
        assertNotNull(actual = reader)
    }
}
