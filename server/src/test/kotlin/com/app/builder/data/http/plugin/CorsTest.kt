package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import com.app.builder.data.http.URL
import com.app.builder.test.TestCase

class CorsTest: TestCase() {

    /** Verifies that, in development mode, requests from the local web hosts are allowed and echo their origin back. */
    @Test
    fun allowsDevelopmentOrigins() = runServerTest {
        installTestRoutes()
        val client = createClient(token = null)

        listOf("http://localhost:8010", "http://${URL.DEV_HOST}").forEach { origin ->
            val response = client.get(urlString = "/test") { header(HttpHeaders.Origin, origin) }
            assertEquals(expected = HttpStatusCode.OK, actual = response.status)
            assertEquals(expected = origin, actual = response.headers[HttpHeaders.AccessControlAllowOrigin])
        }
    }

    /** Verifies that, in development mode, requests from any other origin, including production ones, are refused. */
    @Test
    fun refusesOtherOrigins() = runServerTest {
        installTestRoutes()
        val client = createClient(token = null)

        listOf("https://evil.example", "https://app.example").forEach { origin ->
            val response = client.get(urlString = "/test") { header(HttpHeaders.Origin, origin) }
            assertEquals(expected = HttpStatusCode.Forbidden, actual = response.status)
            assertNull(actual = response.headers[HttpHeaders.AccessControlAllowOrigin])
        }
    }

    /** Verifies that a preflight from an allowed origin is accepted for allowed methods and arbitrary headers, and refused for a method outside the allowed set. */
    @Test
    fun preflight() = runServerTest {
        installTestRoutes()
        val client = createClient(token = null)
        val origin = "http://localhost:8010"

        val allowed = client.options(urlString = "/test") {
            header(HttpHeaders.Origin, origin)
            header(HttpHeaders.AccessControlRequestMethod, HttpMethod.Post.value)
            header(HttpHeaders.AccessControlRequestHeaders, "X-Custom-Header")
        }
        assertEquals(expected = HttpStatusCode.OK, actual = allowed.status)
        assertEquals(expected = origin, actual = allowed.headers[HttpHeaders.AccessControlAllowOrigin])

        val refused = client.options(urlString = "/test") {
            header(HttpHeaders.Origin, origin)
            header(HttpHeaders.AccessControlRequestMethod, HttpMethod.Delete.value)
        }
        assertEquals(expected = HttpStatusCode.Forbidden, actual = refused.status)
    }

    /** Installs the CORS plugin and a test route answering GET and POST. */
    private fun ApplicationTestBuilder.installTestRoutes() {
        application {
            installCors()
            routing {
                get(path = "/test") { call.respondText(text = "ok") }
                post(path = "/test") { call.respondText(text = "ok") }
            }
        }
    }
}
