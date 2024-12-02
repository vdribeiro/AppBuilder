package com.app.builder.data.http

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.ClientSSESession
import io.ktor.client.plugins.sse.serverSentEvents
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import io.ktor.http.encodeURLQueryComponent
import io.ktor.sse.ServerSentEvent
import io.ktor.websocket.Frame

/**
 * Executes an HTTP GET request.
 *
 * @param request The request data.
 * @return The [HttpResponse].
 */
suspend fun HttpClient.get(
    request: HttpRequest
): HttpResponse = get(urlString = request.url.path.encodeURLPath()) {
    request.headerMap.forEach { header(key = it.key.header, value = it.value) }
    request.queryMap.forEach { url.parameters.append(name = it.key.parameter, value = it.value) }
}

/**
 * Executes an HTTP POST request.
 *
 * @param B The type of the serialized request body payload.
 * @param request The request data.
 * @param body The optional content payload sent in the HTTP request body.
 * @return The [HttpResponse].
 */
suspend inline fun <reified B> HttpClient.post(
    request: HttpRequest,
    body: B? = null,
): HttpResponse = post(urlString = request.url.path.encodeURLPath()) {
    request.headerMap.forEach { header(key = it.key.header, value = it.value) }
    request.queryMap.forEach { url.parameters.append(name = it.key.parameter, value = it.value) }
    contentType(type = ContentType.Application.Json)
    setBody(body = body)
}

/**
 * Establishes an SSE connection to the specified endpoint and listens for incoming events.
 *
 * @param request The request data.
 * @param block A callback function invoked for every incoming event.
 */
suspend fun HttpClient.sse(
    request: HttpRequest,
    onConnected: suspend ClientSSESession.() -> Unit,
    block: suspend ClientSSESession.(ServerSentEvent) -> Unit,
) = serverSentEvents(urlString = request.url.path.encodeURLPath()) {
    onConnected()
    incoming.collect { event -> block(event) }
}

/**
 * Establishes a WebSocket connection to the specified endpoint and listens for incoming frames.
 * This function suspends until the connection is closed. Any network failures or exceptions thrown during frame collection will propagate to the caller.
 *
 * @param T The specific subtype of WebSocket [Frame] to filter and collect.
 * @param request The request data.
 * @param block A callback function invoked for every incoming frame that matches the defined type [T].
 */
suspend inline fun <reified T: Frame> HttpClient.webSocket(
    request: HttpRequest,
    crossinline onConnected: suspend DefaultClientWebSocketSession.() -> Unit,
    crossinline block: suspend DefaultClientWebSocketSession.(T) -> Unit,
) {
    val query = request.queryMap.entries.joinToString(separator = "&") { (key, value) ->
        "${key.parameter.encodeURLQueryComponent()}=${value.encodeURLQueryComponent()}"
    }.let { if (it.isNotBlank()) "?$it" else "" }
    webSocket(urlString = "${request.url.path.encodeURLPath()}$query") {
        onConnected()
        val job = launch {
            incoming.consumeAsFlow()
                .filterIsInstance<T>()
                .collect { frame -> block(frame) }
        }
        closeReason.await()
        job.cancelAndJoin()
    }
}
