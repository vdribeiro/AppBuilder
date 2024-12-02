package com.app.builder.data.http.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.PipelineCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import com.app.builder.core.locale.now
import com.app.builder.core.platform.Property
import com.app.builder.data.http.Header

/** Configures a plugin to inject headers and validate outgoing and incoming network requests. */
fun Application.installInterceptor() {
    install(plugin = createApplicationPlugin(name = "Interceptor") {
        onCall { call ->
            val now = now().toString()
            call.headerIfAbsent(key = Header.ServerArriveUtc, value = now)
        }
        onCallRespond { call ->
            val now = now().toString()
            call.headerIfAbsent(key = Header.ServerName, value = Property.serverName)
            call.headerIfAbsent(key = Header.ServerVersion, value = Property.serverVersion)
            call.headerIfAbsent(key = Header.ServerSentUtc, value = now)
        }
    })
}

/**
 * Adds a header to the request only if it is not already present.
 *
 * @param key The name of the header.
 * @param value The value of the header.
 */
private fun PipelineCall.headerIfAbsent(key: Header, value: String?) {
    if (value != null && !response.headers.contains(key.header)) response.headers.append(name = key.header, value = value)
}
