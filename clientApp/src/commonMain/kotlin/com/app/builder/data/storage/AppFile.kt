package com.app.builder.data.storage

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import com.app.builder.core.config.ClientConfigs
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.config.ServerFlags
import com.app.builder.ui.component.bar.ActionBarMode

/** The index of every file in this module. */
object AppFile {

    /** User scoped preferences. */
    data object Preferences: StorageFile<Map<Preferences.Key, String>>(path = "preferences") {

        @Serializable
        enum class Key {
            /** The UUID of the user currently authenticated on this device. */
            CURRENT_USER
        }

        override val serializer: KSerializer<Map<Key, String>> =
            MapSerializer(keySerializer = Key.serializer(), valueSerializer = String.serializer())
    }

    /** Watermarks recording when each entity type was last synchronized with the server. */
    data object Sync: StorageFile<Map<Sync.Key, String>>(path = "sync") {

        @Serializable
        enum class Key {
            /** The UTC instant when registries were last synchronized. */
            REGISTRIES_LAST_SYNC_UTC,
            /** The UTC instant when users were last synchronized. */
            USERS_LAST_SYNC_UTC,
            /** The UTC instant when tasks were last synchronized. */
            TASKS_LAST_SYNC_UTC
        }

        override val serializer: KSerializer<Map<Key, String>> =
            MapSerializer(keySerializer = Key.serializer(), valueSerializer = String.serializer())
    }

    /** Client feature flags. */
    data object ClientFeatureFlags: StorageFile<ClientFlags>(path = "client_flags", cache = false) {
        override val serializer: KSerializer<ClientFlags> = ClientFlags.serializer()
    }

    /** Client configs. */
    data object ClientRemoteConfigs: StorageFile<ClientConfigs>(path = "client_configs", cache = false) {
        override val serializer: KSerializer<ClientConfigs> = ClientConfigs.serializer()
    }

    /** Server feature flags. */
    data object ServerFeatureFlags: StorageFile<ServerFlags>(path = "server_flags", cache = false) {
        override val serializer: KSerializer<ServerFlags> = ServerFlags.serializer()
    }

    /** Server configs. */
    data object ServerRemoteConfigs: StorageFile<ServerConfigs>(path = "server_configs", cache = false) {
        override val serializer: KSerializer<ServerConfigs> = ServerConfigs.serializer()
    }

    /**
     * Criteria used to filter, sort, and shape a list-detail view.
     *
     * @property mode Current display mode, controlling which actions/inputs are shown.
     * @property search Search query text.
     * @property sortProperty Name of the property used to sort the list.
     * @property sortAscending Whether the list is sorted in ascending order.
     * @property visibleProperties Names of the property values shown as visible columns.
     * @property searchableProperties Names of the property values included when searching.
     * @property selectedUuids UUIDs the user has selected.
     */
    @Serializable
    data class FilterCriteria(
        val mode: ActionBarMode = ActionBarMode.DEFAULT,
        val search: String = "",
        val sortProperty: String = "",
        val sortAscending: Boolean = false,
        val visibleProperties: ImmutableList<String> = persistentListOf(),
        val searchableProperties: ImmutableList<String> = persistentListOf(),
        val selectedUuids: ImmutableList<String> = persistentListOf(),
    )

    /** Task preferences. */
    data object TaskPreferences: StorageFile<FilterCriteria>(path = "task_preferences", encrypted = false) {

        override val serializer: KSerializer<FilterCriteria> = FilterCriteria.serializer()
    }

    /** Every file in this module. */
    val all: List<StorageFile<*>> = listOf(
        Preferences,
        Sync,
        ClientFeatureFlags,
        ClientRemoteConfigs,
        ServerFeatureFlags,
        ServerRemoteConfigs,
        TaskPreferences
    )
}
