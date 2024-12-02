package com.app.builder.core.platform

/**
 * Retrieves the value of the specified environment variable.
 *
 * This function wraps the system call in [runCatching] to gracefully handle potential exceptions.
 *
 * @param name The name of the environment variable.
 * @return The string value of the environment variable, or `null` if the variable is not defined or an error occurs during retrieval.
 */
fun getEnv(name: String): String? = runCatching { System.getenv(name) }.getOrNull()

/**
 * Retrieves the value of the specified system property.
 *
 * This function wraps the system call in [runCatching] to gracefully handle potential exceptions.
 *
 * @param name The name of the system property.
 * @return The string value of the system property, or `null` if the property is not defined or an error occurs during retrieval.
 */
fun getProperty(name: String): String? = runCatching { System.getProperty(name) }.getOrNull()
