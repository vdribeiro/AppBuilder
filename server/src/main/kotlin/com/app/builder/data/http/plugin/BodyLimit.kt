package com.app.builder.data.http.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.request.path
import com.app.builder.core.config.ServerConfigs
import com.app.builder.data.http.URL

/** Installs a limit on the size of request bodies: [ServerConfigs.maxFileSize] on the file routes and [ServerConfigs.maxBodySize] on every other route. A bigger body is refused with 413 Payload Too Large. */
fun Application.installBodyLimit() {
    install(plugin = RequestBodyLimit) {
        bodyLimit { call ->
            when {
                call.request.path().startsWith(prefix = URL.Files.path) -> ServerConfigs.value.maxFileSize
                else -> ServerConfigs.value.maxBodySize
            }
        }
    }
}
