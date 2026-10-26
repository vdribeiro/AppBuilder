package com.app.builder.data.http.plugin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import com.app.builder.core.locale.toInstant
import com.app.builder.core.platform.Property
import com.app.builder.data.http.Header
import com.app.builder.test.TestCase

class InterceptorTest: TestCase() {

    /** Verifies that every response carries the server arrive and sent times, and the server name and version when they are configured. */
    @Test
    fun injectsServerHeaders() = runServerTest {
        application {
            installInterceptor()
            routing { get(path = "/test") { call.respondText(text = "ok") } }
        }
        val client = createClient(token = null)

        val response = client.get(urlString = "/test")
        assertEquals(expected = HttpStatusCode.OK, actual = response.status)
        assertNotNull(actual = response.headers[Header.ServerArriveUtc.header]?.toInstant())
        assertNotNull(actual = response.headers[Header.ServerSentUtc.header]?.toInstant())
        assertEquals(expected = Property.serverName, actual = response.headers[Header.ServerName.header])
        assertEquals(expected = Property.serverVersion, actual = response.headers[Header.ServerVersion.header])
    }

    /** Verifies that a header already set by the route is kept rather than overwritten or duplicated. */
    @Test
    fun keepsHeadersSetByTheRoute() = runServerTest {
        application {
            installInterceptor()
            routing {
                get(path = "/test") {
                    call.response.header(name = Header.ServerSentUtc.header, value = "route")
                    call.respondText(text = "ok")
                }
            }
        }
        val client = createClient(token = null)

        val response = client.get(urlString = "/test")
        assertEquals(expected = listOf("route"), actual = response.headers.getAll(name = Header.ServerSentUtc.header))
    }
}
