package com.app.builder.core.nfc

import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.app.builder.domain.EntityType

/**
 * A tag read from an NFC session.
 *
 * @property id The tag's serial number, or an empty string on platforms that don't expose a generic tag identifier.
 * @property records The [NfcRecord]s decoded from the tag's NDEF message.
 */
data class NfcTag(val id: String, val records: List<NfcRecord>)

/** An instruction triggered by tapping an NFC tag written by this app. A tag can carry multiple records to combine behaviors. */
@Serializable
sealed interface NfcRecord {

    /**
     * Navigates to the [entityType] list screen if [entityUuid] is null, or its detail screen otherwise.
     *
     * @property entityType The domain type of the entity.
     * @property entityUuid The unique UUID of the entity.
     */
    @Serializable
    @SerialName(value = "open")
    data class Open(
        val entityType: EntityType,
        val entityUuid: Uuid? = null
    ): NfcRecord

    /**
     * Inserts a new [entityType] or updates the existing one.
     *
     * @property entityType The domain type of the entity.
     * @property entityUuid The unique UUID of the entity.
     */
    @Serializable
    @SerialName(value = "upsert")
    data class Upsert(
        val entityType: EntityType,
        val entityUuid: Uuid? = null
    ): NfcRecord
}