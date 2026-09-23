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

/**
 * Lists every file held under [path], including the ones in its subdirectories.
 *
 * @param path The absolute path of the directory to list, typically the application data or cache directory.
 * @return The files found, or an empty list if the directory does not exist or cannot be read.
 */
expect suspend fun listFiles(path: String): List<DeviceFile>

/**
 * Hands [path] over to the platform's default handler for its type, so the file opens outside the application.
 *
 * @param path The absolute path of the file to open.
 * @return `true` if a handler was launched, `false` if the platform has no handler for it or the hand off failed.
 */
expect suspend fun openFile(path: String): Boolean

/**
 * A file held in one of the application's own directories.
 *
 * @property path The absolute path of the file, or the storage key on platforms without a file system.
 * @property name The file name, including its extension.
 * @property size The size of the file in bytes.
 * @property modifiedAt The epoch milliseconds of the last modification, or null when the platform does not track it.
 */
data class DeviceFile(
    val path: String,
    val name: String,
    val size: Long,
    val modifiedAt: Long?
)
