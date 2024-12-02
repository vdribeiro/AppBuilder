package com.app.builder.data.http

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class HeaderTest: TestCase() {

    /** Verifies each Header constant maps to its expected raw HTTP header name. */
    @Test
    fun headerValues() = runUnitTest {
        assertEquals(expected = "X-Server-Name", actual = Header.ServerName.header)
        assertEquals(expected = "X-Server-Version", actual = Header.ServerVersion.header)
        assertEquals(expected = "X-Server-Arrive-Utc", actual = Header.ServerArriveUtc.header)
        assertEquals(expected = "X-Server-Sent-Utc", actual = Header.ServerSentUtc.header)
        assertEquals(expected = "X-Request-UUID", actual = Header.RequestUuid.header)
        assertEquals(expected = "X-Request-UTC", actual = Header.RequestUtc.header)
        assertEquals(expected = "X-Request-Sent-UTC", actual = Header.RequestSentUtc.header)
        assertEquals(expected = "X-App-Name", actual = Header.AppName.header)
        assertEquals(expected = "X-App-Version", actual = Header.AppVersion.header)
        assertEquals(expected = "X-OS", actual = Header.Os.header)
        assertEquals(expected = "X-OS-Version", actual = Header.OsVersion.header)
        assertEquals(expected = "X-Brand", actual = Header.Brand.header)
        assertEquals(expected = "X-Model", actual = Header.Model.header)
        assertEquals(expected = "X-Device-UUID", actual = Header.DeviceUuid.header)
    }
}
