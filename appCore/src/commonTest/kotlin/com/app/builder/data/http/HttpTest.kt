package com.app.builder.data.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import com.app.builder.data.http.plugin.installContentNegotiation
import com.app.builder.data.http.plugin.installSse
import com.app.builder.data.http.plugin.installWebSocket
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Translation
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class HttpTest: TestCase() {

    private val translation = Translation(languageIso = "en", key = "key", value = "value")

    /** Verifies GET succeeds and attaches request headers and query parameters. */
    @Test
    fun getReturnsSuccessAndAttachesHeadersAndQuery() = runUnitTest {
        var capturedHeader: String? = null
        var capturedQuery: String? = null
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                capturedHeader = request.headers[Header.AppName.header]
                capturedQuery = request.url.parameters[Query.DeviceUuid.parameter]
                respond(
                    content = encode(value = translation).orEmpty(),
                    headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
                )
            }
        })) {
            installContentNegotiation()
        }

        val result = client.get<Translation>(
            request = HttpRequest(
                url = URL.Translations,
                headerMap = mapOf(Header.AppName to "app"),
                queryMap = mapOf(Query.DeviceUuid to "device-uuid")
            )
        )

        assertIs<HttpResult.Success<Translation>>(value = result)
        assertEquals(expected = translation, actual = result.data)
        assertEquals(expected = "app", actual = capturedHeader)
        assertEquals(expected = "device-uuid", actual = capturedQuery)
    }

    /** Verifies GET returns an error result when the request fails. */
    @Test
    fun getReturnsErrorOnFailure() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { throw IllegalStateException("Crash") }
        }))

        val result = client.get<Translation>(request = HttpRequest(url = URL.Translations))

        assertIs<HttpResult.Error<Translation>>(value = result)
    }

    /** Verifies POST sends a JSON content type and returns a success result. */
    @Test
    fun postSendsJsonContentTypeAndReturnsSuccess() = runUnitTest {
        var capturedContentType: String? = null
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                capturedContentType = request.body.contentType?.toString()
                respond(
                    content = encode(value = translation).orEmpty(),
                    headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
                )
            }
        })) {
            installContentNegotiation()
        }

        val result = client.post<Translation, Translation>(request = HttpRequest(url = URL.Translations), body = translation)

        assertIs<HttpResult.Success<Translation>>(value = result)
        assertEquals(expected = translation, actual = result.data)
        assertTrue(actual = capturedContentType.orEmpty().startsWith(prefix = "application/json"))
    }

    /** Verifies POST returns an error result when the request fails. */
    @Test
    fun postReturnsErrorOnFailure() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { throw IllegalStateException("Crash") }
        }))

        val result = client.post<Translation, Translation>(request = HttpRequest(url = URL.Translations), body = translation)

        assertIs<HttpResult.Error<Translation>>(value = result)
    }

    /** Verifies SSE invokes onConnected then receives decoded events. */
    @Test
    fun sseInvokesOnConnectedThenReceivesDecodedEvents() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondSse(pushPayload = FakeData.notification) }
        })) {
            installSse()
        }

        var connected = false
        var received: String? = null
        client.sse(
            request = HttpRequest(url = URL.Push),
            onConnected = { connected = true },
            block = { event -> received = event.data }
        )

        assertTrue(actual = connected)
        assertEquals(expected = FakeData.notification, actual = decode(value = received.orEmpty()))
    }

    /** Verifies SSE rethrows a CancellationException thrown in the block. */
    @Test
    fun sseRethrowsCancellationException() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondSse(pushPayload = FakeData.notification) }
        })) {
            installSse()
        }

        assertFailsWith<CancellationException> {
            client.sse(
                request = HttpRequest(url = URL.Push),
                onConnected = {},
                block = { throw CancellationException("Crash") }
            )
        }
    }

    /** Verifies the WebSocket invokes onConnected then receives decoded frames. */
    @Test
    fun webSocketInvokesOnConnectedThenReceivesDecodedFrames() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondWebSocket(pushPayload = FakeData.notification) }
        })) {
            installWebSocket()
        }

        var connected = false
        var received: String? = null
        client.webSocket<Frame.Text>(
            request = HttpRequest(url = URL.Push),
            onConnected = { connected = true },
            block = { frame -> received = frame.readText() }
        )

        assertTrue(actual = connected)
        assertEquals(expected = FakeData.notification, actual = decode(value = received.orEmpty()))
    }

    /** Verifies that [HttpClient.get] with a trailing entity uuid appends it as a path segment. */
    @Test
    fun getWithUuidAppendsUuidAsPathSegment() = runUnitTest {
        var capturedPath: String? = null
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                capturedPath = request.url.encodedPath
                respond(
                    content = encode(value = translation).orEmpty(),
                    headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
                )
            }
        })) {
            installContentNegotiation()
        }

        val uuid = Uuid.random()
        val result = client.get<Translation>(request = HttpRequest(url = URL.Tasks), uuid = uuid)

        assertIs<HttpResult.Success<Translation>>(value = result)
        assertTrue(actual = capturedPath.orEmpty().endsWith(suffix = "/$uuid"))
    }

    /** Verifies that [HttpClient.head] issues a HEAD request. */
    @Test
    fun headIssuesAHeadRequest() = runUnitTest {
        var capturedMethod: String? = null
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                capturedMethod = request.method.value
                respond(content = "")
            }
        }))

        client.head(request = HttpRequest(url = URL.Probe))

        assertEquals(expected = "HEAD", actual = capturedMethod)
    }

    /** Verifies that [HttpClient.head] swallows any failure instead of throwing. */
    @Test
    fun headSwallowsFailures() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { throw IllegalStateException("Crash") }
        }))

        client.head(request = HttpRequest(url = URL.Probe))
    }

    /** A fixture item used to exercise [HttpClient.getPaginated]'s cursor advance and terminal condition. */
    @Serializable
    private data class PageItem(val utc: String, val uuid: String)

    /** Verifies that [HttpClient.getPaginated] advances the cursor from the last item of each page and stops once a page is shorter than the requested page size. */
    @Test
    fun getPaginatedAdvancesCursorAcrossPagesUntilShortPage() = runUnitTest {
        val firstPage = listOf(PageItem(utc = "3", uuid = "c"), PageItem(utc = "2", uuid = "b"))
        val secondPage = listOf(PageItem(utc = "1", uuid = "a"))
        var callCount = 0
        val capturedCursors = mutableListOf<Pair<String?, String?>>()
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                callCount++
                capturedCursors.add(
                    element = request.url.parameters[Query.CursorUtc.parameter] to request.url.parameters[Query.CursorUuid.parameter]
                )
                val items = if (callCount == 1) firstPage else secondPage
                respond(
                    content = encode(value = items).orEmpty(),
                    headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
                )
            }
        })) {
            installContentNegotiation()
        }

        val pages = mutableListOf<List<PageItem>>()
        val result = client.getPaginated<PageItem>(
            request = HttpRequest(url = URL.Tasks),
            pageSize = 2,
            cursorOf = { it.utc to it.uuid },
            onPage = { items -> pages.add(element = items); HttpResult.Success(data = Unit) }
        )

        assertIs<HttpResult.Success<Unit>>(value = result)
        assertEquals(expected = listOf(firstPage, secondPage), actual = pages)
        assertEquals(expected = 2, actual = callCount)
        assertEquals(expected = listOf(null to null, "2" to "b"), actual = capturedCursors)
    }

    /** Verifies that [HttpClient.getPaginated] aborts pagination as soon as [onPage] returns an error, without requesting further pages. */
    @Test
    fun getPaginatedAbortsEarlyWhenOnPageReturnsError() = runUnitTest {
        val firstPage = listOf(PageItem(utc = "2", uuid = "b"), PageItem(utc = "1", uuid = "a"))
        var callCount = 0
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add {
                callCount++
                respond(
                    content = encode(value = firstPage).orEmpty(),
                    headers = headersOf(HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()))
                )
            }
        })) {
            installContentNegotiation()
        }

        val error = IllegalStateException("stop")
        val result = client.getPaginated<PageItem>(
            request = HttpRequest(url = URL.Tasks),
            pageSize = 2,
            cursorOf = { it.utc to it.uuid },
            onPage = { HttpResult.Error(error = error) }
        )

        assertIs<HttpResult.Error<Unit>>(value = result)
        assertEquals(expected = error, actual = result.error)
        assertEquals(expected = 1, actual = callCount)
    }

    /** Verifies that [HttpClient.getPaginated] surfaces a network failure as an error without invoking [onPage]. */
    @Test
    fun getPaginatedReturnsErrorOnNetworkFailure() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { throw IllegalStateException("Crash") }
        }))

        var onPageInvoked = false
        val result = client.getPaginated<PageItem>(
            request = HttpRequest(url = URL.Tasks),
            pageSize = 2,
            cursorOf = { it.utc to it.uuid },
            onPage = { onPageInvoked = true; HttpResult.Success(data = Unit) }
        )

        assertIs<HttpResult.Error<Unit>>(value = result)
        assertTrue(actual = !onPageInvoked)
    }
}
