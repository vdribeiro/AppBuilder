package com.app.builder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.app.builder.core.flow.Dispatcher
import com.app.builder.domain.nfc.AndroidTagProvider
import com.app.builder.test.ExcludeFromTesting

/**
 * Transparent, no-UI entry point for a NFC tag tapped while [MainActivity] isn't the one claiming it (app backgrounded or not running).
 * Sends to [AndroidTagProvider] to decode the tag off the app's normal lifecycle, so a physical tap never forces the full UI open by itself.
 */
@ExcludeFromTesting
class NfcActivity: Activity() {

    /** Scope for working with nfc tags. */
    private val scope = CoroutineScope(context = SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent = intent)
    }

    /**
     * Handle an [intent].
     *
     * @param intent to handle.
     */
    private fun handleIntent(intent: Intent) {
        scope.launch(context = Dispatcher.IO) {
            val result = AndroidTagProvider.handleNfcTag(intent = intent)
            withContext(context = Dispatchers.Main) {
                if (result) launchMainActivity()
                finish()
            }
        }
    }

    /** Brings [MainActivity] forward, reusing the existing task instead of starting a new one. */
    private fun launchMainActivity() {
        packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }?.let { startActivity(it) }
    }
}
