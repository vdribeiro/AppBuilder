package com.app.builder.data.http

import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.sse.ClientSSESession
import io.ktor.client.plugins.sse.serverSentEvents
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import io.ktor.http.encodeURLQueryComponent
import io.ktor.sse.ServerSentEvent
import io.ktor.websocket.Frame
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.developmentMode
import com.app.builder.data.http.URL.Companion.DEV_HOST
import com.app.builder.data.http.URL.Companion.HOST

/**
 * Executes a type-safe HTTP HEAD request.
 *
 * @param request The request data.
 */
suspend fun HttpClient.head(
    request: HttpRequest
) = withContext(context = Dispatcher.IO) {
    runCatching {
        head(urlString = "$baseUrl${request.url.path.encodeURLPath()}") {
            request.headerMap.forEach { header(key = it.key.header, value = it.value) }
            request.queryMap.forEach { url.parameters.append(name = it.key.parameter, value = it.value) }
        }
    }.let {}
}

/**
 * Executes a type-safe HTTP GET request.
 *
 * @param T The expected response model type.
 * @param request The request data.
 * @return A [HttpResult.Success] containing [T] on success, or a [HttpResult.Error] encapsulating the caught exception.
 */
suspend inline fun <reified T> HttpClient.get(
    request: HttpRequest
): HttpResult<T> = withContext(context = Dispatcher.IO) {
    runCatching {
        val response = get(urlString = "$baseUrl${request.url.path.encodeURLPath()}") {
            request.headerMap.forEach { header(key = it.key.header, value = it.value) }
            request.queryMap.forEach { url.parameters.append(name = it.key.parameter, value = it.value) }
        }
        HttpResult.Success(data = response.body<T>())
    }.getOrElse { HttpResult.Error(error = it) }
}

/**
 * Executes a type-safe HTTP GET request for a single entity, addressed by [uuid] as a trailing path segment.
 *
 * @param T The expected response model type.
 * @param request The request data.
 * @param uuid The unique identifier of the entity to fetch, appended to [request]'s path.
 * @return A [HttpResult.Success] containing [T] on success, or a [HttpResult.Error] encapsulating the caught exception.
 */
suspend inline fun <reified T> HttpClient.get(
    request: HttpRequest,
    uuid: Uuid
): HttpResult<T> = withContext(context = Dispatcher.IO) {
    runCatching {
        val response = get(urlString = "$baseUrl${request.url.path.encodeURLPath()}/$uuid") {
            request.headerMap.forEach { header(key = it.key.header, value = it.value) }
            request.queryMap.forEach { url.parameters.append(name = it.key.parameter, value = it.value) }
        }
        HttpResult.Success(data = response.body<T>())
    }.getOrElse { HttpResult.Error(error = it) }
}

/**
 * Executes a series of type-safe HTTP GET requests, following a descending keyset cursor one page at a time (newest items first) until a page returns fewer items than [pageSize], meaning no data remains.
 *
 * @param T The expected item type per page.
 * @param request The base request.
 * @param pageSize The number of items requested per page.
 * @param cursorOf Extracts the [Query.CursorUtc] and [Query.CursorUuid] pair from an item, used to continue to the next page.
 * @param onPage Invoked with each page's items, in order. Return [HttpResult.Error] to abort pagination early.
 * @return [HttpResult.Success] once every page has been consumed, or the first [HttpResult.Error] encountered from the network or from [onPage].
 */
suspend inline fun <reified T> HttpClient.getPaginated(
    request: HttpRequest,
    pageSize: Int,
    cursorOf: (T) -> Pair<String, String>,
    onPage: suspend (List<T>) -> HttpResult<Unit>
): HttpResult<Unit> {
    var cursorUtc: String? = null
    var cursorUuid: String? = null

    do {
        val queryMap = buildMap {
            putAll(from = request.queryMap)
            cursorUtc?.let { put(key = Query.CursorUtc, value = it) }
            cursorUuid?.let { put(key = Query.CursorUuid, value = it) }
            put(key = Query.PageSize, value = pageSize.toString())
        }
        val items = when (val result = get<List<T>>(request = request.copy(queryMap = queryMap))) {
            is HttpResult.Error -> return HttpResult.Error(error = result.error)
            is HttpResult.Success -> result.data
        }
        when (val result = onPage(items)) {
            is HttpResult.Error -> return result
            is HttpResult.Success -> Unit
        }
        items.lastOrNull()?.let { last ->
            val (utc, uuid) = cursorOf(last)
            cursorUtc = utc
            cursorUuid = uuid
        }
    } while (items.size == pageSize)

    return HttpResult.Success(data = Unit)
}

/**
 * Executes a type-safe HTTP POST request.
 *
 * @param B The type of the serialized request body payload.
 * @param T The expected response model type.
 * @param request The request data.
 * @param body The optional content payload sent in the HTTP request body.
 * @return A [HttpResult.Success] containing [T] on success, or a [HttpResult.Error] encapsulating the caught exception.
 */
suspend inline fun <reified B, reified T> HttpClient.post(
    request: HttpRequest,
    body: B? = null,
): HttpResult<T> = withContext(context = Dispatcher.IO) {
    runCatching {
        val response = post(urlString = "$baseUrl${request.url.path.encodeURLPath()}") {
            request.headerMap.forEach { header(key = it.key.header, value = it.value) }
            request.queryMap.forEach { url.parameters.append(name = it.key.parameter, value = it.value) }
            contentType(type = ContentType.Application.Json)
            setBody(body = body)
        }
        HttpResult.Success(data = response.body<T>())
    }.getOrElse { HttpResult.Error(error = it) }
}

/**
 * Establishes an SSE connection to the specified endpoint and listens for incoming events.
 * This function suspends until the connection is closed. Any network failures or exceptions thrown during event collection will propagate to the caller.
 *
 * @param request The request data.
 * @param onConnected A callback function invoked after a successful connection.
 * @param block A callback function invoked for every incoming event.
 */
suspend fun HttpClient.sse(
    request: HttpRequest,
    onConnected: suspend ClientSSESession.() -> Unit,
    block: suspend ClientSSESession.(ServerSentEvent) -> Unit,
) = withContext(context = Dispatcher.IO) {
    runCatching {
        serverSentEvents(urlString = "$baseUrl${request.url.path.encodeURLPath()}") {
            onConnected()
            incoming.collect { event -> block(event) }
        }
    }.onFailure {
        if (it is CancellationException) throw it
    }
}

/**
 * Establishes a WebSocket connection to the specified endpoint and listens for incoming frames.
 * This function suspends until the connection is closed. Any network failures or exceptions thrown during frame collection will propagate to the caller.
 *
 * @param T The specific subtype of WebSocket [Frame] to filter and collect.
 * @param request The request data.
 * @param onConnected A callback function invoked after a successful connection.
 * @param block A callback function invoked for every incoming frame that matches the defined type [T].
 */
suspend inline fun <reified T: Frame> HttpClient.webSocket(
    request: HttpRequest,
    crossinline onConnected: suspend DefaultClientWebSocketSession.() -> Unit,
    crossinline block: suspend DefaultClientWebSocketSession.(T) -> Unit,
) = withContext(context = Dispatcher.IO) {
    runCatching {
        val query = request.queryMap.entries.joinToString(separator = "&") { (key, value) ->
            "${key.parameter.encodeURLQueryComponent()}=${value.encodeURLQueryComponent()}"
        }.let { if (it.isNotBlank()) "?$it" else "" }
        webSocket(urlString = "$baseWSUrl${request.url.path.encodeURLPath()}$query") {
            onConnected()
            incoming.consumeAsFlow()
                .filterIsInstance<T>()
                .collect { frame -> block(frame) }
        }
    }.onFailure {
        if (it is CancellationException) throw it
    }
}

/** The active base URL evaluated at runtime. */
val baseUrl: String = if (developmentMode) "${URLProtocol.HTTP.name}://$DEV_HOST" else "${URLProtocol.HTTPS.name}://$HOST"

/** The active base WebSocket URL evaluated at runtime. */
val baseWSUrl: String = if (developmentMode) "${URLProtocol.WS.name}://$DEV_HOST" else "${URLProtocol.WSS.name}://$HOST"
