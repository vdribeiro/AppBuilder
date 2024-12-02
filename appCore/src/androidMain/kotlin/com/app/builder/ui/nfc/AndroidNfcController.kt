package com.app.builder.ui.nfc

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.app.builder.core.nfc.AndroidNfcController
import com.app.builder.core.nfc.NfcController
import com.app.builder.ui.lifecycle.Register

@Composable
internal actual fun RegisterNfcControllerLifecycle(nfcController: NfcController) {
    val androidNfcController = nfcController as? AndroidNfcController ?: return
    val activity = LocalActivity.current
    var state by remember { mutableStateOf(value = androidNfcController.state.value) }
    androidNfcController.set(activity = activity)
    Register(
        androidNfcController,
        onBackground = {
            state = nfcController.state.value
            androidNfcController.onBackground()
        },
        onForeground = {
            when (state) {
                NfcController.State.Idle -> Unit
                NfcController.State.Scanning,
                NfcController.State.Writing -> androidNfcController.onForeground()
            }
        },
        onDispose = {
            androidNfcController.dispose()
        }
    )
}
