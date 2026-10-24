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
    /** The TLS mode for the database connection. */
    val dbSslMode: String? = getEnv(name = "DB_SSL_MODE")
    /** The issuer claim expected in JWT. */
    val jwtIssuer: String? = getEnv(name = "JWT_ISSUER")
    /** The audience claim expected in JWT. */
    val jwtAudience: String? = getEnv(name = "JWT_AUDIENCE")
    /** The secret key used to sign and verify JWT. */
    val jwtSecret: String? = getEnv(name = "JWT_SECRET")
    /** The GCP project ID. */
    val gcpProjectId: String? = getEnv(name = "GCP_PROJECT_ID")
    /** The number of trusted reverse proxies sitting in front of the server, used to resolve the caller's address for rate limiting. Zero, the default, means the server is reached directly and `X-Forwarded-For` is not trusted at all. */
    val trustedProxyHops: Int = getEnv(name = "TRUSTED_PROXY_HOPS")?.toIntOrNull() ?: 0
    /** The base address of the S3 compatible object storage, without a bucket or trailing slash. */
    val s3Endpoint: String? = getEnv(name = "S3_ENDPOINT")
    /** The object storage bucket holding uploaded files. */
    val s3Bucket: String? = getEnv(name = "S3_BUCKET")
    /** The region the object storage bucket lives in, used to scope request signatures. */
    val s3Region: String? = getEnv(name = "S3_REGION")
    /** The object storage access key id. Travels in every signed request and is not a secret. */
    val s3AccessKeyId: String? = getEnv(name = "S3_ACCESS_KEY_ID")
    /** The object storage secret used to sign requests. */
    val s3SecretAccessKey: String? = getEnv(name = "S3_SECRET_ACCESS_KEY")
}
