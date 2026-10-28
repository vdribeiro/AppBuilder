package com.app.builder.data.storage

import java.awt.Desktop
import java.io.File
import java.io.OutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlinx.coroutines.withContext
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.platform.appCachePath
import com.app.builder.core.platform.appDataPath
import com.app.builder.core.telemetry.Telemetry

actual suspend fun saveFile(path: String, content: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = File(appDataPath, path)
        file.writeAtomically { stream -> stream.write(content.encodeToByteArray()) }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to save file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun loadFile(path: String): String? = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = File(appDataPath, path)
        if (file.exists() && file.isFile) file.readText() else null
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to load file $path", throwable = it)
    }.getOrNull()
}

actual suspend fun deleteFile(path: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = File(appDataPath, path)
        !file.exists() || file.delete()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to delete file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun clearCache(): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        File(appCachePath).listFiles()?.forEach { it.deleteRecursively() }
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to clear cache", throwable = it)
    }.getOrDefault(defaultValue = false)
}

actual suspend fun listFiles(path: String): List<DeviceFile> = withContext(context = Dispatcher.IO) {
    runCatching {
        File(path).walkTopDown()
            .filter { it.isFile }
            .map { DeviceFile(path = it.absolutePath, name = it.name, size = it.length(), modifiedAt = it.lastModified()) }
            .toList()
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to list files in $path", throwable = it)
    }.getOrDefault(defaultValue = emptyList())
}

actual suspend fun openFile(path: String): Boolean = withContext(context = Dispatcher.IO) {
    runCatching {
        val file = File(path)
        if (!file.exists()) error(message = "File does not exist")
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) error(message = "No desktop handler available")
        Desktop.getDesktop().open(file)
        true
    }.onFailure {
        Telemetry.error(tag = TAG, message = "Unable to open file $path", throwable = it)
    }.getOrDefault(defaultValue = false)
}

/**
 * Replaces this file with the bytes written by [write], so the file holds either its previous or its new content, never a partial one.
 * The content goes to a temporary file in the same directory, is flushed to disk, and is then moved over this file in one step.
 *
 * @param write Writes the new content to the given stream.
 */
internal fun File.writeAtomically(write: (OutputStream) -> Unit) {
    val temporary = File.createTempFile(".$name.", ".tmp", parentFile)
    try {
        temporary.outputStream().use { stream ->
            write(stream)
            stream.fd.sync()
        }
        Files.move(temporary.toPath(), toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        temporary.delete()
    }
}

/**
 * Runs [block] while holding an exclusive lock on [file], shared with every other process of the app.
 *
 * @param file The lock file, created if missing.
 * @param block The work to run under the lock.
 * @return The result of [block].
 */
internal inline fun <T> withFileLock(file: File, block: () -> T): T =
    FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
        channel.lock().use { block() }
    }

private const val TAG = "File"
