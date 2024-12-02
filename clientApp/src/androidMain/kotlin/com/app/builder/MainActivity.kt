package com.app.builder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.app.builder.domain.push.AndroidPushProvider
import com.app.builder.test.ExcludeFromTesting
import com.app.builder.ui.App

/**
 * The Android entry point activity.
 * Starts the application and renders the Compose UI.
 */
@ExcludeFromTesting
class MainActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Application.start()
        handleIntent(intent = intent)
        enableEdgeToEdge()
        setContent { App() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent = intent)
    }

    /**
     * Handle an [intent].
     *
     * @param intent to handle.
     */
    private fun handleIntent(intent: Intent) {
        AndroidPushProvider.handleNotificationPayload(intent = intent)
    }
}
