package com.app.builder.data.device

import kotlin.uuid.Uuid
import com.app.builder.core.security.toUuid
import com.app.builder.core.security.uuid
import com.app.builder.data.storage.CoreFile

/**
 * Retrieves the device UUID from local storage.
 * If a UUID is not currently present, a new one is generated, safely persisted to storage, and then returned.
 *
 * @return The unique device identifier.
 */
suspend fun getDeviceUuid(): Uuid {
    CoreFile.Device.cache().value.orEmpty()[CoreFile.Device.Key.UUID]?.toUuid()?.let { return it }
    return uuid().also { uuid -> CoreFile.Device.save { it.orEmpty().plus(pair = (CoreFile.Device.Key.UUID to uuid.toString())) } }
}
