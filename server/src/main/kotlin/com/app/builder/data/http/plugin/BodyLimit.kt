package com.app.builder.data.http.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import com.app.builder.core.config.ServerConfigs

/** Installs a limit of [ServerConfigs.maxBodySize] on the size of request bodies. A bigger body is refused with 413 Payload Too Large. */
fun Application.installBodyLimit() {
    install(plugin = RequestBodyLimit) {
        bodyLimit { ServerConfigs.value.maxBodySize }
    }
}
