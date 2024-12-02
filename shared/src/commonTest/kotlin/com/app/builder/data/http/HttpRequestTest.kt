package com.app.builder.data.http

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class HttpRequestTest: TestCase() {

    /** Verifies a newly constructed HttpRequest has the given url and empty header and query maps. */
    @Test
    fun defaults() = runUnitTest {
        val request = HttpRequest(url = URL.Probe)
        assertEquals(expected = URL.Probe, actual = request.url)
        assertEquals(expected = emptyMap(), actual = request.headerMap)
        assertEquals(expected = emptyMap(), actual = request.queryMap)
    }

    /** Verifies a constructed HttpRequest stores non-empty header and query maps as given. */
    @Test
    fun nonEmptyMaps() = runUnitTest {
        val headerMap = mapOf(Header.AppName to "AppBuilder", Header.DeviceUuid to "device-uuid")
        val queryMap = mapOf(Query.PageSize to "20", Query.CursorUtc to "2024-01-01T00:00:00Z")

        val request = HttpRequest(url = URL.Probe, headerMap = headerMap, queryMap = queryMap)

        assertEquals(expected = headerMap, actual = request.headerMap)
        assertEquals(expected = queryMap, actual = request.queryMap)
    }
}
