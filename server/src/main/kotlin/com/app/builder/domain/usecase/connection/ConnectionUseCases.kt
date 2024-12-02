package com.app.builder.domain.usecase.connection

import kotlin.uuid.Uuid

/**
 * Defines the business workflows for WebSocket connection presence.
 * Presence is persisted so any server instance can see which devices hold a live connection anywhere, even though the sockets themselves are bound to the memory of a single instance.
 * Rows are kept alive by a periodic heartbeat; rows whose heartbeat has lapsed are treated as dead and swept.
 */
interface ConnectionUseCases {

    /**
     * Registers a device as connected to a specific server instance, enforcing the global and per-user connection limits.
     * A device reconnecting replaces its own previous presence row, so a zombie row never blocks a reconnect.
     *
     * @param userUuid The user owning the connection.
     * @param deviceUuid The device holding the connection.
     * @param instanceId The server instance owning the socket.
     * @return `true` if the presence was registered, `false` if a limit was reached or persistence failed.
     */
    suspend fun addConnection(userUuid: Uuid, deviceUuid: Uuid, instanceId: Uuid): Boolean

    /**
     * Removes a device's presence row, but only if it is still owned by the given instance.
     * A row already re-claimed by a newer connection on another instance is left untouched.
     *
     * @param deviceUuid The device to remove.
     * @param instanceId The server instance requesting the removal.
     * @return `true` if a row was removed, `false` otherwise.
     */
    suspend fun removeConnection(deviceUuid: Uuid, instanceId: Uuid): Boolean

    /**
     * Retrieves the devices of a user that currently hold a live connection on any instance.
     * Rows whose heartbeat has lapsed are not considered live.
     *
     * @param userUuid The target user's UUID.
     * @return The list of connected device UUIDs.
     */
    suspend fun getConnectedDeviceUuids(userUuid: Uuid): List<Uuid>

    /**
     * Refreshes the heartbeat of every presence row owned by the given instance, and sweeps dead rows left behind by crashed instances.
     *
     * @param instanceId The server instance whose rows to refresh.
     */
    suspend fun touchConnections(instanceId: Uuid)
}
