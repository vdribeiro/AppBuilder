package com.app.builder.data.http

/** Header constants used for communication and telemetry. */
sealed class Header(val header: String) {
    /** The server application name. */
    data object ServerName: Header(header = "X-Server-Name")
    /** The server application version. */
    data object ServerVersion: Header(header = "X-Server-Version")
    /** The UTC timestamp of the server when the request arrived. */
    data object ServerArriveUtc: Header(header = "X-Server-Arrive-Utc")
    /** The UTC timestamp of the server when the response was sent. */
    data object ServerSentUtc: Header(header = "X-Server-Sent-Utc")

    /** The request unique identifier. */
    data object RequestUuid: Header(header = "X-Request-UUID")
    /** The UTC timestamp of when the request was made. */
    data object RequestUtc: Header(header = "X-Request-UTC")
    /** The UTC timestamp of when the request was sent. */
    data object RequestSentUtc: Header(header = "X-Request-Sent-UTC")
    /** The client application name. */
    data object AppName: Header(header = "X-App-Name")
    /** The client application version. */
    data object AppVersion: Header(header = "X-App-Version")
    /** The operating system family of the client device. */
    data object Os: Header(header = "X-OS")
    /** The operating system version of the client device. */
    data object OsVersion: Header(header = "X-OS-Version")
    /** The manufacturer or brand of the client device. */
    data object Brand: Header(header = "X-Brand")
    /** The hardware model of the client device. */
    data object Model: Header(header = "X-Model")
    /** The device's installation UUID. */
    data object DeviceUuid: Header(header = "X-Device-UUID")
}