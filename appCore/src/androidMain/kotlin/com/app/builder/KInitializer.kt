package com.app.builder

import android.content.Context
import android.content.ContextWrapper
import androidx.startup.Initializer

/**
 * Reference to the application's [Context] initialized via [KInitializer] at application startup.
 * This allows components within this module to access resources and system services without requiring explicit context injection.
 */
var applicationContext: Context = ContextWrapper(null)

/** An [Initializer] implementation that automatically captures the application context during the early stages of the app's lifecycle and stores it in the [applicationContext] property. */
class KInitializer: Initializer<Unit> {
    override fun create(context: Context) {
        applicationContext = context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
