package com.app.builder.data.storage

/** Every file in the application. */
val files: List<StorageFile<*>> = CoreFile.all + AppFile.all

/**
 * Logs a string with the registered files.
 *
 * @return log output.
 */
fun logFiles(): String = if (files.isNotEmpty()) buildString {
    appendLine(value = "Registered files:")
    val paths = files.map { it.path }
    append(paths.joinToString(separator = "\n"))
    if (paths.size != paths.distinctBy { it.lowercase() }.size) append("\nWARNING: DUPLICATE PATHS!")
} else "No files to register"
