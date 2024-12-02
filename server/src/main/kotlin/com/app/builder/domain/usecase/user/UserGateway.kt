package com.app.builder.domain.usecase.user

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.UserTable
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.User

/**
 * Gateway implementation for users use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class UserGateway(
    private val database: R2dbcDatabase,
): UserUseCases {

    override suspend fun getUsers(
        lastSyncUtc: Instant,
        cursorUtc: Instant?,
        cursorUuid: Uuid?,
        pageSize: Int
    ): List<User> = withContext(context = Dispatcher.IO) {
        val effectivePageSize = pageSize.coerceIn(minimumValue = 1, maximumValue = ServerConfigs.configs.maxPageSize)
        database.safeTransaction {
            UserTable
                .selectAll()
                .where {
                    val sinceLastSync = UserTable.modifiedAt greaterEq lastSyncUtc
                    when {
                        cursorUtc == null || cursorUuid == null -> sinceLastSync
                        else -> sinceLastSync and ((UserTable.modifiedAt less cursorUtc) or ((UserTable.modifiedAt eq cursorUtc) and (UserTable.uuid less cursorUuid)))
                    }
                }
                .orderBy(UserTable.modifiedAt to SortOrder.DESC, UserTable.uuid to SortOrder.DESC)
                .limit(count = effectivePageSize)
                .toList()
                .map { it.toUser() }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get users since $lastSyncUtc", throwable = it)
        }.getOrDefault(defaultValue = emptyList())
    }

    override suspend fun getUser(uuid: Uuid): User? = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            UserTable
                .selectAll()
                .where { UserTable.uuid eq uuid }
                .firstOrNull()
                ?.toUser()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get user $uuid", throwable = it)
        }.getOrNull()
    }

    override suspend fun upsertUser(user: User): Boolean = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            UserTable.upsert {
                it[UserTable.uuid] = user.uuid
                it[UserTable.modifiedAt] = user.modifiedAt
                it[UserTable.deletedAt] = user.deletedAt
                it[UserTable.permissions] = encode(value = user.permissions).orEmpty()
                it[UserTable.name] = user.name
                it[UserTable.avatar] = user.avatar
            }.insertedCount > 0
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert user ${user.uuid}", throwable = it)
        }.getOrDefault(defaultValue = false)
    }

    /**
     * Maps a database row to a [User] domain model.
     *
     * @receiver A row from [UserTable].
     * @return The mapped [User].
     */
    private fun ResultRow.toUser(): User = User(
        uuid = this[UserTable.uuid],
        modifiedAt = this[UserTable.modifiedAt],
        deletedAt = this[UserTable.deletedAt],
        permissions = decode<Map<EntityType, Permission>>(value = this[UserTable.permissions]).orEmpty(),
        name = this[UserTable.name],
        avatar = this[UserTable.avatar]
    )

    companion object {
        private const val TAG = "UserGateway"
    }
}
