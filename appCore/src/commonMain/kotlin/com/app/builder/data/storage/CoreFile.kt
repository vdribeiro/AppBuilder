package com.app.builder.data.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** The index of every file in this module. */
object CoreFile {

    /** Device scoped identity and clock bookkeeping, written by the device, clock and networking infrastructure. */
    data object Device: StorageFile<Map<Device.Key, String>>(path = "device") {

        @Serializable
        enum class Key {
            /** The stable identifier generated once per installation. */
            UUID,
            /** The measured drift between the device clock and the server clock. */
            CLOCK_OFFSET
        }

        override val serializer: KSerializer<Map<Key, String>> = MapSerializer(keySerializer = Key.serializer(), valueSerializer = String.serializer())
    }

    /** Every file in this module. */
    val all: List<StorageFile<*>> = listOf(
        Device
    )
}
