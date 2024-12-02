package com.app.builder

/** Application values provided by the consuming app. */
object App {
    /** Application id. */
    var id: String = ""
        private set
    /** Application name. */
    var name: String = ""
        private set
    /** Application version. */
    var version: String = ""
        private set

    /**
     * Sets the application identity.
     *
     * @param id The application id.
     * @param name The application name.
     * @param version The application version.
     */
    fun setInfo(id: String, name: String, version: String) {
        this.id = id
        this.name = name
        this.version = version
    }
}