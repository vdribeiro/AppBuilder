package com.app.builder.data.storage

/**
 * Persists a [String] payload to the device's local file system at the specified [path].
 *
 * @param path The absolute destination file path.
 * @param content The string data to write.
 * @return `true` if the write operation completes successfully, `false` if an I/O error occurs or permissions are denied.
 */
expect suspend fun saveFile(path: String, content: String): Boolean

/**
 * Reads and retrieves the contents of a local file as a [String].
 *
 * @param path The absolute path of the file to load.
 * @return The file content as a string, or null if the file does not exist or cannot be read.
 */
expect suspend fun loadFile(path: String): String?

/**
 * Permanently removes a file from the device storage.
 *
 * @param path The absolute path of the file to delete.
 * @return `true` if the file was successfully deleted or did not exist, `false` if the operation fails due to system errors.
 */
expect suspend fun deleteFile(path: String): Boolean

/**
 * Deletes every file in the application's cache directory.
 *
 * @return `true` if the operation completes successfully, `false` if an I/O error occurs or permissions are denied.
 */
expect suspend fun clearCache(): Boolean
