package com.app.builder.data.http.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import com.app.builder.core.platform.Property
import com.app.builder.core.security.createVerifier

/**
 * Installs and configures the Authentication plugin for the Ktor [Application].
 * Configures JWT-based authentication utilizing the application's environment variables for audience and issuer validation.
 *
 * @throws Throwable if the JWT audience or issuer environment variables are missing or blank.
 */
fun Application.installAuthentication() {
    install(plugin = Authentication) {
        jwt {
            realm = Property.serverName.orEmpty()
            verifier(verifier = createVerifier())
            validate { credential -> JWTPrincipal(payload = credential.payload) }
        }
    }
}
