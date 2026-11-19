package com.app.builder.data.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** The index of every file in this module. */
object CoreFile {

    /** Device scoped identity, clock bookkeeping and app language. */
    data object Device: StorageFile<Map<Device.Key, String>>(path = "device") {

        @Serializable
        enum class Key {
            /** The stable identifier generated once per installation. */
            UUID,
            /** The measured drift between the device clock and the server clock. */
            CLOCK_OFFSET,
            /** The ISO 639-1 code of the language selected for the app, absent while the app follows the device language. */
            LANGUAGE
        }

        override val serializer: KSerializer<Map<Key, String>> = MapSerializer(keySerializer = Key.serializer(), valueSerializer = String.serializer())
    }

    /** Every file in this module. */
    val all: List<StorageFile<*>> = listOf(
        Device
    )
}
