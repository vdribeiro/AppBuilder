package com.app.builder.domain.push

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.app.builder.core.flow.Dispatcher

/** Provider to manage push payloads. */
object FcmMessaging {

    /** An isolated scope for the provider's coroutine operations. */
    private val scope = CoroutineScope(context = SupervisorJob())

    /** Starts the provider. */
    fun start() {
        scope.launch(context = Dispatcher.Default) {
            WebPushProvider.observeToken()
        }

        scope.launch(context = Dispatcher.Default) {
            WebPushProvider.observeMessages(scope = scope)
        }
    }
}
