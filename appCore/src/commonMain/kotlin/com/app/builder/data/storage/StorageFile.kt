package com.app.builder.data.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.security.decrypt
import com.app.builder.core.security.encrypt
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode

/**
 * A file holding a payload of type [T], with an optional cache layer.
 *
 * @param T The payload type held by this file.
 * @property path The filename assigned to this file, resolved relative to the platform application data directory.
 * @property cache Whether this file should have a cache layer for swift read access.
 * @property encrypted Whether the payload is encrypted at rest. Enabled by default.
 * @property serializer The strategy describing how to read and write the payload.
 */
abstract class StorageFile<T>(
    val path: String,
    val cache: Boolean = true,
    val encrypted: Boolean = true
) {
    /** The strategy describing how to read and write the payload. */
    abstract val serializer: KSerializer<T>

    /** Mutex guarding cache creation to ensure instance uniqueness. */
    private val mutex = Mutex()

    /** Backing state flow maintaining the active volatile layer. */
    private val _state: MutableStateFlow<T?> = MutableStateFlow(value = null)

    /** Whether the cache has already been hydrated. */
    private var hydrated: Boolean = false

    /**
     * Resets cache if enabled and permanently deletes the physical file.
     *
     * @return `true` if the operation completes successfully, `false` if an I/O error occurs or permissions are denied.
     */
    suspend fun reset(): Boolean = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            if (cache) {
                _state.update { null }
                hydrated = true
            }
            deleteFile(path = path)
        }
    }

    /**
     * Dispatches an atomic transformation to the volatile payload if the cache is enabled, then commits the result to the device's persistent file system.
     * If the given payload is null, then it permanently deletes the physical file.
     * A failed commit rolls the volatile payload back, so the cache never advertises a payload the file system did not accept.
     *
     * @param function A lambda block that receives the current payload snapshot and returns the updated payload.
     * @return `true` if the operation completes successfully, `false` if an I/O error occurs or permissions are denied.
     */
    suspend fun save(function: (T?) -> T?): Boolean = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            val previous = if (cache) {
                hydrate()
                _state.value
            } else readFromDisk()

            val value = if (cache) _state.updateAndGet(function = function) else function(previous)
            (if (value == null) deleteFile(path) else writeToDisk(value = value)).also { success -> if (cache && !success) _state.update { previous } }
        }
    }

    /**
     * Read from the device's persistent file system.
     * If the cache is enabled, the volatile layer is updated and marked as hydrated.
     *
     * @return The persisted payload of type [T], or `null` if the file does not exist, is unreadable, or fails decryption/decoding.
     */
    suspend fun load(): T? = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            readFromDisk().also { result ->
                if (cache) {
                    _state.update { result }
                    hydrated = true
                }
            }
        }
    }

    /**
     * Resolves the reactive cache flow fronting the file.
     *
     * @return The cache flow bound to the file.
     */
    suspend fun cache(): StateFlow<T?> = withContext(context = Dispatcher.IO) {
        mutex.withLock {
            hydrate()
            _state.asStateFlow()
        }
    }

    /**
     * Serializes [value], encrypts it if the file declares it, and commits it to disk.
     *
     * @param value The payload to persist.
     * @return `true` if the operation completes successfully, `false` otherwise.
     */
    private suspend fun writeToDisk(value: T): Boolean =
        encode(serializer = serializer, value = value)
            ?.let { if (encrypted) encrypt(content = it) else it }
            ?.let { saveFile(path = path, content = it) }
            ?: false

    /**
     * Reads the file from disk, decrypts it when the file declares it, and deserializes the payload.
     *
     * @return The persisted payload, or `null` if the file is absent or unreadable.
     */
    private suspend fun readFromDisk(): T? =
        loadFile(path = path)
            ?.let { if (encrypted) decrypt(content = it) else it }
            ?.let { decode(serializer = serializer, value = it) }

    /** Reads from disk and populates the volatile cache if caching is enabled and not yet hydrated. */
    private suspend fun hydrate() {
        if (cache && !hydrated) {
            val result = readFromDisk()
            _state.update { result }
            hydrated = true
        }
    }
}
