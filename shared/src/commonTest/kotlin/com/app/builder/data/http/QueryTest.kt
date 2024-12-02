package com.app.builder.data.http

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.test.TestCase

class QueryTest: TestCase() {

    /** Verifies each Query constant maps to its expected raw query parameter name. */
    @Test
    fun queryValues() = runUnitTest {
        assertEquals(expected = "last_sync_utc", actual = Query.LastSyncUtc.parameter)
        assertEquals(expected = "cursor_utc", actual = Query.CursorUtc.parameter)
        assertEquals(expected = "cursor_uuid", actual = Query.CursorUuid.parameter)
        assertEquals(expected = "page_size", actual = Query.PageSize.parameter)
        assertEquals(expected = "device_uuid", actual = Query.DeviceUuid.parameter)
        assertEquals(expected = "ticket_uuid", actual = Query.TicketUuid.parameter)
    }
}
