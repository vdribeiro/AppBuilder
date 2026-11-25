package com.app.builder.data.http

import io.ktor.server.application.Application
import com.app.builder.data.http.plugin.installAuthentication
import com.app.builder.data.http.plugin.installBodyLimit
import com.app.builder.data.http.plugin.installCompression
import com.app.builder.data.http.plugin.installCors
import com.app.builder.data.http.plugin.installInterceptor
import com.app.builder.data.http.plugin.installLogging
import com.app.builder.data.http.plugin.installRateLimit
import com.app.builder.data.http.plugin.installSerialization
import com.app.builder.data.http.plugin.installSse
import com.app.builder.data.http.plugin.installWebSocket

/** Installs the required plugins for the HTTP server. */
fun Application.installHttpPlugins() {
    installLogging()
    installBodyLimit()
    installSerialization()
    installCompression()
    installInterceptor()
    installAuthentication()
    installWebSocket()
    installSse()
    installCors()
    installRateLimit()
}