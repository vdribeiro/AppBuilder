package com.app.builder.data.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer

/** Files registered solely by the storage test suite. */
object TestFile {

    /**
     * Payload used by the document file fixtures.
     *
     * @property name The document's name.
     * @property count The document's count.
     */
    @Serializable
    data class Document(val name: String, val count: Int)

    /** A key-value fixture with its own key set. */
    data object Keys: StorageFile<Map<Keys.Key, String>>(path = "test-keys") {
        @Serializable
        enum class Key { ALPHA, BETA }

        override val serializer: KSerializer<Map<Key, String>> =
            MapSerializer(keySerializer = Key.serializer(), valueSerializer = String.serializer())
    }

    /** An encrypted document fixture. */
    data object Secret: StorageFile<Document?>(path = "test-secret") {
        override val serializer: KSerializer<Document?> = Document.serializer().nullable
    }

    /** Every file registered by the test suite. */
    val all: List<StorageFile<*>> = listOf(Keys, Secret)
}
