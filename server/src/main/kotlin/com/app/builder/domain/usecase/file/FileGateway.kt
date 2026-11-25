package com.app.builder.domain.usecase.file

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.utils.io.ByteReadChannel
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.flow.runSuspendCatching
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.FileTable
import com.app.builder.domain.FileMetadata
import com.app.builder.domain.file.FileStorage
import com.app.builder.domain.usecase.Outcome

/**
 * Gateway implementation for file use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property fileStorage The storage holding the content of files.
 */
class FileGateway(
    private val database: R2dbcDatabase,
    private val fileStorage: FileStorage
): FileUseCases {

    override suspend fun getFile(uuid: Uuid): Outcome<FileMetadata> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            FileTable
                .selectAll()
                .where { FileTable.uuid eq uuid }
                .firstOrNull()
                ?.toFileMetadata()
                ?.let { Outcome.Success(data = it) }
                ?: Outcome.NotFound
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get file $uuid", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun uploadFile(file: FileMetadata, content: ByteReadChannel): Outcome<FileMetadata> = withContext(context = Dispatcher.IO) {
        val size = runSuspendCatching {
            fileStorage.write(uuid = file.uuid, content = content)
        }.getOrElse {
            if (it is PayloadTooLargeException) throw it
            Telemetry.error(tag = TAG, message = "Unable to store the content of file ${file.uuid}", throwable = it)
            return@withContext Outcome.Error
        } ?: run {
            Telemetry.error(tag = TAG, message = "Unable to store the content of file ${file.uuid}")
            return@withContext Outcome.Error
        }

        val stored = file.copy(size = size)
        database.safeTransaction {
            FileTable.upsert {
                it[FileTable.uuid] = stored.uuid
                it[FileTable.modifiedAt] = stored.modifiedAt
                it[FileTable.deletedAt] = stored.deletedAt
                it[FileTable.name] = stored.name
                it[FileTable.contentType] = stored.contentType
                it[FileTable.size] = stored.size
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert file ${stored.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false).let { if (it) Outcome.Success(data = stored) else Outcome.Error }
    }

    override suspend fun readFile(uuid: Uuid): Outcome<ByteReadChannel> = withContext(context = Dispatcher.IO) {
        runSuspendCatching {
            fileStorage.read(uuid = uuid)
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to read the content of file $uuid", throwable = it)
            return@withContext Outcome.Error
        }?.let { Outcome.Success(data = it) } ?: Outcome.NotFound
    }

    /**
     * Maps a database row to a [FileMetadata] domain model.
     *
     * @receiver A row from the [FileTable].
     * @return The mapped [FileMetadata].
     */
    private fun ResultRow.toFileMetadata(): FileMetadata = FileMetadata(
        uuid = this[FileTable.uuid],
        modifiedAt = this[FileTable.modifiedAt],
        deletedAt = this[FileTable.deletedAt],
        name = this[FileTable.name],
        contentType = this[FileTable.contentType],
        size = this[FileTable.size]
    )

    companion object {
        private const val TAG = "FileGateway"
    }
}
