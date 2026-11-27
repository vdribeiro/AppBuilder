package com.app.builder.core.locale

import kotlinx.coroutines.flow.Flow

/**
 * Returns the ISO 639-1 two-letter code of the device language.
 *
 * @return The device language code (e.g., "en", "pt").
 */
expect fun getDeviceLanguage(): String

/**
 * Observes the device language.
 *
 * @return A [Flow] emitting the ISO 639-1 code of the device language, and the new one whenever it changes.
 */
expect fun observeDeviceLanguage(): Flow<String>
