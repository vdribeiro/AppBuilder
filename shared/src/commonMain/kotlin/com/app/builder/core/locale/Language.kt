package com.app.builder.core.locale

import com.app.builder.core.flow.StateHolder

/** Holds the ISO 639-1 code of the language selected for the app, or `null` while the app follows the device language. */
object Language: StateHolder<String?>(initialValue = null)
