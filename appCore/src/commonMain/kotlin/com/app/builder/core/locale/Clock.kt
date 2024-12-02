package com.app.builder.core.locale

import kotlinx.coroutines.flow.Flow

/**
 * Observe system clock changes.
 *
 * @return A [Flow] that emits [Unit] whenever the OS reports a system clock change.
 */
expect fun observeClockChanges(): Flow<Unit>
