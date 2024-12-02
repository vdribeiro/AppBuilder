package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.core.telemetry.TelemetryEngine
import com.app.builder.data.http.Header
import com.app.builder.data.http.TestEngine
import com.app.builder.data.http.baseUrl
import com.app.builder.data.http.respondMock
import com.app.builder.data.storage.CoreFile
import com.app.builder.test.TestCase

class InterceptorTest: TestCase() {

    /** Verifies requests throw when the network flag is disabled. */
    @Test
    fun throwsWhenNetworkDisabled() = runUnitTest {
        ClientFlags.set { it.copy(http = false) }

        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock() }
        })) {
            installInterceptor()
        }

        assertFailsWith<NetworkDisabledException> {
            client.get(urlString = baseUrl)
        }
    }

    /** Verifies requests throw when the internet is unavailable. */
    @Test
    fun throwsWhenInternetUnavailable() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock() }
        })) {
            installInterceptor(isInternetAvailable = { false })
        }

        assertFailsWith<InternetDisabledException> {
            client.get(urlString = baseUrl)
        }
    }

    /** Verifies default headers are injected while existing headers set by the caller are respected. */
    @Test
    fun injectsDefaultHeadersAndRespectsExisting() = runUnitTest {
        var headers: Headers? = null
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                headers = request.headers
                respondMock()
            }
        })) {
            installInterceptor()
        }

        client.get(urlString = baseUrl) {
            header(key = Header.AppName.header, value = "custom-app-name")
        }

        val sentHeaders = assertNotNull(actual = headers)
        assertEquals(expected = "application/json", actual = sentHeaders[HttpHeaders.Accept])
        assertTrue(actual = sentHeaders[Header.RequestUuid.header]!!.isNotBlank())
        assertTrue(actual = sentHeaders[Header.RequestUtc.header]!!.isNotBlank())
        assertTrue(actual = sentHeaders[Header.RequestSentUtc.header]!!.isNotBlank())
        assertTrue(actual = sentHeaders[Header.DeviceUuid.header]!!.isNotBlank())
        assertEquals(expected = "custom-app-name", actual = sentHeaders[Header.AppName.header])
    }

    /** Verifies an unauthorized response causes an unauthorized exception to be thrown. */
    @Test
    fun throwsUnauthorizedOnUnauthorizedResponse() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock(status = HttpStatusCode.Unauthorized) }
        })) {
            installInterceptor()
        }

        assertFailsWith<UnauthorizedException> {
            client.get(urlString = baseUrl)
        }
    }

    /** Verifies an error response status causes an unsuccessful status exception to be thrown. */
    @Test
    fun throwsUnsuccessfulStatusOnErrorResponse() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock(status = HttpStatusCode.InternalServerError) }
        })) {
            installInterceptor()
        }

        assertFailsWith<UnsuccessfulStatusException> {
            client.get(urlString = baseUrl)
        }
    }

    /** Verifies a not-modified response does not throw. */
    @Test
    fun doesNotThrowOnNotModifiedResponse() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock(status = HttpStatusCode.NotModified) }
        })) {
            installInterceptor()
        }

        val response = client.get(urlString = baseUrl)

        assertEquals(expected = HttpStatusCode.NotModified, actual = response.status)
    }

    /** Verifies a switching-protocols response does not throw. */
    @Test
    fun doesNotThrowOnSwitchingProtocolsResponse() = runUnitTest {
        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock(status = HttpStatusCode.SwitchingProtocols) }
        })) {
            installInterceptor()
        }

        val response = client.get(urlString = baseUrl)

        assertEquals(expected = HttpStatusCode.SwitchingProtocols, actual = response.status)
    }

    /** Verifies the clock offset is synchronized from server response headers. */
    @Test
    fun synchronizesClockFromServerHeaders() = runUnitTest {
        assertNull(actual = CoreFile.Device.cache().value.orEmpty()[CoreFile.Device.Key.CLOCK_OFFSET])

        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { request ->
                val sentUtc = request.headers[Header.RequestSentUtc.header].orEmpty()
                respond(
                    content = "",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                        Header.ServerArriveUtc.header to listOf(sentUtc),
                        Header.ServerSentUtc.header to listOf(sentUtc),
                    )
                )
            }
        })) {
            installInterceptor()
        }

        client.get(urlString = baseUrl)

        assertNotNull(actual = CoreFile.Device.cache().value.orEmpty()[CoreFile.Device.Key.CLOCK_OFFSET])
    }

    /** Verifies a failure is reported through telemetry when clock headers are missing from the response. */
    @Test
    fun reportsFailureWhenClockHeadersAreMissing() = runUnitTest {
        val messages = mutableListOf<String>()
        Telemetry.engines.add(element = object: TelemetryEngine {
            override fun info(tag: String, message: String) {}
            override fun error(tag: String, message: String, throwable: Throwable?) {
                if (tag == "Interceptor") messages.add(message)
            }

            override fun feedback(message: String) {}
        })

        val client = HttpClient(engine = TestEngine(config = MockEngineConfig().apply {
            requestHandlers.add { respondMock() }
        })) {
            installInterceptor()
        }

        client.get(urlString = baseUrl)

        assertTrue(actual = messages.isNotEmpty())
    }
}
