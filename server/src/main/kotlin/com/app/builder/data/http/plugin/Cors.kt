package com.app.builder.data.http.plugin

import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import com.app.builder.core.platform.Env
import com.app.builder.data.http.URL.Companion.DEV_HOST
import com.app.builder.data.http.URL.Companion.HOST

/** Configures Cross-Origin Resource Sharing (CORS) for the Ktor server. */
fun Application.installCors() {
    install(plugin = CORS) {
        when {
            Env.developmentMode -> {
                allowHost(host = "localhost:8010")
                allowHost(host = DEV_HOST)
            }

            else -> allowHost(host = HOST, schemes = listOf("https"))
        }

        allowHeaders { true }

        listOf(
            HttpMethod.Options,
            HttpMethod.Head,
            HttpMethod.Get,
            HttpMethod.Post,
        ).forEach { allowMethod(method = it) }

        allowNonSimpleContentTypes = true
    }
}
