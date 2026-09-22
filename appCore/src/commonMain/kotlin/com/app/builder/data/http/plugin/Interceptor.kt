package com.app.builder.data.http.plugin

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import com.app.builder.App
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.locale.now
import com.app.builder.core.locale.toInstant
import com.app.builder.core.platform.platform
import com.app.builder.core.security.uuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.device.getDeviceUuid
import com.app.builder.data.http.Header
import com.app.builder.data.http.isInternetAvailable
import com.app.builder.data.storage.CoreFile

/**
 * Configures a plugin to inject headers and validate outgoing and incoming network requests.
 * Sets the accepted content type to JSON and appends client telemetry for remote diagnostics.
 *
 * @param T The underlying HTTP client engine configuration type.
 * @param isInternetAvailable Checks whether an active internet connection is currently available.
 * @throws NetworkDisabledException If network requests are globally disabled via feature flags.
 * @throws InternetDisabledException If no active internet connection is detected prior to the request.
 * @throws UnauthorizedException If the response yields a 401 Unauthorized status code.
 * @throws UnsuccessfulStatusException If the response yields a status code outside the 2xx success range, other than 101 Switching Protocols, which allows WebSocket upgrades through, and 304 Not Modified.
 */
internal fun <T: HttpClientEngineConfig> HttpClientConfig<T>.installInterceptor(
    isInternetAvailable: () -> Boolean = ::isInternetAvailable
) {
    install(plugin = createClientPlugin(name = "Interceptor") {
        onRequest { request, _ ->
            if (!ClientFlags.flags.http) throw NetworkDisabledException()
            if (!isInternetAvailable()) throw InternetDisabledException()

            val now = now().toString()
            request.header(key = HttpHeaders.Accept, value = ContentType.Application.Json)
            request.headerIfAbsent(key = Header.RequestUuid, value = uuid().toString())
            request.headerIfAbsent(key = Header.RequestUtc, value = now)
            request.headerIfAbsent(key = Header.RequestSentUtc, value = now)
            request.headerIfAbsent(key = Header.AppName, value = App.name)
            request.headerIfAbsent(key = Header.AppVersion, value = App.version)
            request.headerIfAbsent(key = Header.Os, value = platform.os.toString())
            request.headerIfAbsent(key = Header.OsVersion, value = platform.osVersion)
            request.headerIfAbsent(key = Header.Brand, value = platform.brand)
            request.headerIfAbsent(key = Header.Model, value = platform.model)
            request.headerIfAbsent(key = Header.DeviceUuid, value = getDeviceUuid().toString())
        }
        onResponse { response ->
            if (response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
            if (response.status != HttpStatusCode.SwitchingProtocols && response.status.value !in (200 until 300) && response.status != HttpStatusCode.NotModified) throw UnsuccessfulStatusException(status = response.status)

            val result = response.handleTimeSynchronization()
            if (!result) Telemetry.error(tag = TAG, message = "Unable to synchronize clock")
        }
    })
}

/**
 * Adds a header to the request only if it is not already present.
 *
 * @param key The name of the header.
 * @param value The value of the header.
 */
private fun HttpRequestBuilder.headerIfAbsent(key: Header, value: String?) {
    if (!headers.contains(key.header)) header(key = key.header, value = value)
}

/**
 * Estimates the current server time and client clock offset using an NTP-style round-trip calculation, updates the clock accordingly and saves the results to storage.
 *
 * @return `true` if the clock was successfully synchronized, `false` otherwise.
 */
private suspend fun HttpResponse.handleTimeSynchronization(): Boolean = runCatching {
    val serverArriveUtc = headers[Header.ServerArriveUtc.header]?.toInstant() ?: return false
    val serverSentUtc = headers[Header.ServerSentUtc.header]?.toInstant() ?: return false
    val clientSentUtc = call.request.headers[Header.RequestSentUtc.header]?.toInstant() ?: return false

    val now = now()
    val offset = ((serverArriveUtc - clientSentUtc) + (serverSentUtc - now)) / 2
    return CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.CLOCK_OFFSET to offset.toString())) }
}.onFailure {
    Telemetry.error(tag = TAG, message = "Failed to synchronize clock", throwable = it)
}.getOrDefault(defaultValue = false)

/** Thrown when networking capabilities are programmatically disabled. */
class NetworkDisabledException: Throwable(message = "Network disabled")

/** Thrown when no active internet connection is detected by the device hardware. */
class InternetDisabledException: Throwable(message = "No internet connection available")

/** Thrown when the server responds with a 401 Unauthorized status code. */
class UnauthorizedException: Throwable(message = "Unauthorized response")

/**
 * Thrown when the server responds with a status code outside the acceptable success range.
 *
 * @param status The unsuccessful HTTP status code returned by the server.
 */
class UnsuccessfulStatusException(status: HttpStatusCode): Throwable(message = "Unsuccessful response: ${status.value} ${status.description}")

private const val TAG = "Interceptor"