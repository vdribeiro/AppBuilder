package com.app.builder.data.http

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.currentCoroutineContext
import io.ktor.client.engine.HttpClientEngineCapability
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.sse.SSECapability
import io.ktor.client.plugins.websocket.WebSocketCapability
import io.ktor.client.plugins.websocket.WebSocketExtensionsCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.ResponseAdapterAttributeKey
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import io.ktor.websocket.WebSocketSession
import com.app.builder.data.serializer.encode
import com.app.builder.domain.PushPayload

/**
 * A [MockEngine] with the required capabilities for test cases.
 *
 * @param config The [MockEngineConfig] with the routes to handle.
 */
class TestEngine(config: MockEngineConfig): MockEngine(config = config) {
    override val supportedCapabilities: Set<HttpClientEngineCapability<out Any>> = setOf(
        HttpTimeoutCapability,
        WebSocketCapability,
        WebSocketExtensionsCapability,
        SSECapability,
    )

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val response = super.execute(data = data)
        val body = response.body as? ByteReadChannel ?: return response
        val adaptedBody = data.attributes.getOrNull(key = ResponseAdapterAttributeKey)?.adapt(
            data = data,
            status = response.statusCode,
            headers = response.headers,
            responseBody = body,
            outgoingContent = data.body,
            callContext = response.callContext
        ) ?: return response
        return HttpResponseData(
            statusCode = response.statusCode,
            requestTime = response.requestTime,
            headers = response.headers,
            version = response.version,
            body = adaptedBody,
            callContext = response.callContext,
        )
    }
}

/**
 * Send mock response.
 *
 * @param status [HttpStatusCode] to send.
 * @param content [String] to send.
 * @return [HttpResponseData] to send.
 */
fun MockRequestHandleScope.respondMock(
    status: HttpStatusCode = HttpStatusCode.OK,
    content: String? = ""
): HttpResponseData = respond(
    content = content.orEmpty(),
    status = status,
    headers = headersOf(name = HttpHeaders.ContentType, value = ContentType.Application.Json.toString())
)

/**
 * Send bad request response.
 *
 * @param request [HttpRequestData] to send.
 * @return [HttpResponseData] to send.
 */
fun MockRequestHandleScope.badRequest(request: HttpRequestData): HttpResponseData =
    respondMock(status = HttpStatusCode.BadRequest, content = "Method not found: ${request.method}")

/**
 * Send not found response.
 *
 * @param request [HttpRequestData] to send.
 * @return [HttpResponseData] to send.
 */
fun MockRequestHandleScope.notFound(request: HttpRequestData): HttpResponseData =
    respondMock(status = HttpStatusCode.NotFound, content = "Resource not found for path: ${request.url.encodedPath}")

/**
 * Send SSE response with a push payload event followed by stream end.
 *
 * @param pushPayload The push payload to emit as an SSE event.
 * @return [HttpResponseData] to send.
 */
fun MockRequestHandleScope.respondSse(pushPayload: PushPayload): HttpResponseData = respond(
    content = "data: ${encode(value = pushPayload)}\n\n",
    status = HttpStatusCode.OK,
    headers = headersOf(HttpHeaders.ContentType to listOf("text/event-stream"))
)

/**
 * Send websocket response with a push payload followed by close.
 *
 * @param pushPayload The push payload to emit as a WebSocket text frame.
 * @return [HttpResponseData] to send.
 */
suspend fun respondWebSocket(pushPayload: PushPayload): HttpResponseData {
    val callContext = currentCoroutineContext()
    val session = object: WebSocketSession, CoroutineScope by CoroutineScope(context = callContext + Job()) {
        override val incoming: ReceiveChannel<Frame> = Channel<Frame>(capacity = Channel.UNLIMITED).apply {
            trySend(element = Frame.Text(text = encode(value = pushPayload) ?: ""))
            trySend(element = Frame.Close(reason = CloseReason(code = CloseReason.Codes.NORMAL, message = "")))
            close()
        }
        override val outgoing: SendChannel<Frame> = Channel(capacity = Channel.UNLIMITED)
        override val extensions: List<WebSocketExtension<*>> = emptyList()
        override var masking: Boolean = false
        override var maxFrameSize: Long = Long.MAX_VALUE
        override suspend fun flush() {}
        @Deprecated(message = "Overriding deprecated")
        override fun terminate() {
        }
    }
    return HttpResponseData(
        statusCode = HttpStatusCode.SwitchingProtocols,
        requestTime = GMTDate(),
        headers = headersOf(name = HttpHeaders.Upgrade, value = "websocket"),
        version = HttpProtocolVersion.HTTP_1_1,
        body = session,
        callContext = callContext,
    )
}