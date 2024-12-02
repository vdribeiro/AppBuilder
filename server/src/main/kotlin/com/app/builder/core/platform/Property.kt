package com.app.builder.core.platform

/** System properties index. */
object Property {
    /**
     * The configured name of the server.
     *
     * @return [String] The server name, or null if not set.
     */
    val serverName: String? = getProperty(name = "server.name")

    /**
     * The configured version of the server.
     *
     * @return [String] The server version, or null if not set.
     */
    val serverVersion: String? = getProperty(name = "server.version")
}
