package com.app.builder.domain.usecase.file

import kotlin.uuid.Uuid
import io.ktor.utils.io.ByteReadChannel
import com.app.builder.domain.FileMetadata
import com.app.builder.domain.usecase.Outcome

/** Defines the business workflows for managing files. */
interface FileUseCases {

    /**
     * Gets the metadata of a single file by UUID.
     *
     * @param uuid The file's unique identifier.
     * @return [Outcome.Success] with the [FileMetadata], [Outcome.NotFound] when it does not exist, or [Outcome.Error] when the operation failed.
     */
    suspend fun getFile(uuid: Uuid): Outcome<FileMetadata>

    /**
     * Stores the [content] of a file and inserts or updates its metadata, with the size of the stored content.
     *
     * @param file The [FileMetadata] to be inserted or updated.
     * @param content The content of the file.
     * @return [Outcome.Success] with the stored [FileMetadata], or [Outcome.Error] when the operation failed.
     */
    suspend fun uploadFile(file: FileMetadata, content: ByteReadChannel): Outcome<FileMetadata>

    /**
     * Reads the content of a file by UUID.
     *
     * @param uuid The file's unique identifier.
     * @return [Outcome.Success] with the content, [Outcome.NotFound] when there is none, or [Outcome.Error] when the operation failed.
     */
    suspend fun readFile(uuid: Uuid): Outcome<ByteReadChannel>
}
