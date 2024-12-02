package com.app.builder.domain.gateway.push

import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.http.HttpRequest
import com.app.builder.data.http.HttpResult
import com.app.builder.data.http.URL
import com.app.builder.data.http.post
import com.app.builder.domain.PushPayload

/**
 * Gateway implementation to send push notifications.
 *
 * @property httpClient The HTTP client used for network operations.
 */
class PushGateway(
    private val httpClient: HttpClient,
): PushUseCases {

    override suspend fun push(pushPayload: PushPayload): Boolean = withContext(context = Dispatcher.IO) {
        when {
            pushPayload.userUuid == null -> send(url = URL.Broadcast, pushPayload = pushPayload)
            else -> send(url = URL.Push, pushPayload = pushPayload)
        }
    }

    /**
     * Posts a push notification [pushPayload] to the given [url].
     *
     * @param url The endpoint to send the push notification to.
     * @param pushPayload The push notification payload to send.
     * @return `true` if the request succeeded; `false` otherwise.
     */
    private suspend fun send(url: URL, pushPayload: PushPayload): Boolean =
        when (val result = httpClient.post<PushPayload, String>(request = HttpRequest(url = url), body = pushPayload)) {
            is HttpResult.Success -> true
            is HttpResult.Error -> {
                Telemetry.error(tag = TAG, message = "Unable to send to ${url.path}", throwable = result.error)
                false
            }
        }

    companion object {
        private const val TAG = "PushGateway"
    }
}
