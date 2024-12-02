package com.app.builder.core.platform

/** Environment variables index. */
object Env {
    /** Indicates whether the server is running in development mode. */
    val developmentMode: Boolean = getEnv(name = "DEVELOPMENT") == "true"
    /** Sentry Data Source Name used for error tracking and monitoring. */
    val sentryDsn: String? = getEnv(name = "SENTRY_DSN")
    /** The port number on which the application server will listen. */
    val port: Int? = getEnv(name = "PORT")?.toIntOrNull()
    /** The superuser uuid. */
    val adminUuid: String? = getEnv(name = "ADMIN_UUID")
    /** The superuser username. */
    val adminUsername: String? = getEnv(name = "ADMIN_USERNAME")
    /** The superuser password. */
    val adminPassword: String? = getEnv(name = "ADMIN_PASSWORD")
    /** The host address for the database connection. */
    val dbHost: String? = getEnv(name = "DB_HOST")
    /** The port number for the database connection. */
    val dbPort: Int? = getEnv(name = "DB_PORT")?.toIntOrNull()
    /** The name of the database to connect to. */
    val dbName: String? = getEnv(name = "DB_NAME")
    /** The username for the database connection. */
    val dbUser: String? = getEnv(name = "DB_USER")
    /** The password for the database connection. */
    val dbPassword: String? = getEnv(name = "DB_PASSWORD")
    /** The issuer claim expected in JWT. */
    val jwtIssuer: String? = getEnv(name = "JWT_ISSUER")
    /** The audience claim expected in JWT. */
    val jwtAudience: String? = getEnv(name = "JWT_AUDIENCE")
    /** The secret key used to sign and verify JWT. */
    val jwtSecret: String? = getEnv(name = "JWT_SECRET")
    /** The GCP project ID. */
    val gcpProjectId: String? = getEnv(name = "GCP_PROJECT_ID")
}
