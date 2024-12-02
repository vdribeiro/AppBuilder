package com.app.builder.core.locale

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSystemClockDidChangeNotification
import com.app.builder.core.platform.observe

actual fun observeClockChanges(): Flow<Unit> = callbackFlow {
    val observer = NSNotificationCenter.defaultCenter.observe(
        name = NSSystemClockDidChangeNotification,
    ) { trySend(element = Unit) }
    awaitClose { NSNotificationCenter.defaultCenter.removeObserver(observer = observer) }
}
