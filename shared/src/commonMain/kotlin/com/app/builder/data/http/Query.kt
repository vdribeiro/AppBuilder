package com.app.builder.data.http

/** The query parameter keys. */
sealed class Query(val parameter: String) {
    /** The last sync UTC timestamp; the fixed lower bound below which results are already synced. */
    data object LastSyncUtc: Query(parameter = "last_sync_utc")
    /** The UTC timestamp of the last entry consumed in the previous page, for descending cursor continuation. */
    data object CursorUtc: Query(parameter = "cursor_utc")
    /** The UUID of the last entry consumed in the previous page, tie-breaking [CursorUtc]. */
    data object CursorUuid: Query(parameter = "cursor_uuid")
    /** The number of entries to return in a single page. */
    data object PageSize: Query(parameter = "page_size")
    /** The device's installation UUID. */
    data object DeviceUuid: Query(parameter = "device_uuid")
    /** The ticket UUID for connection authentication. */
    data object TicketUuid: Query(parameter = "ticket_uuid")
}