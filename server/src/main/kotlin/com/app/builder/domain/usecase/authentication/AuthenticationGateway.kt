package com.app.builder.domain.usecase.authentication

import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.security.createRefreshToken
import com.app.builder.core.security.hashPassword
import com.app.builder.core.security.uuid
import com.app.builder.core.security.verifyPassword
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.UserSecretTable
import com.app.builder.data.database.table.UserSessionTable
import com.app.builder.data.database.table.UserTable
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.permission.createAccessToken

/**
 * Gateway implementation for authentication use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class AuthenticationGateway(
    private val database: R2dbcDatabase
): AuthenticationUseCases {

    override suspend fun register(registrationForm: RegistrationForm): Authentication? = withContext(context = Dispatcher.IO) {
        val user = registrationForm.user
        val credentials = registrationForm.credentials
        val password = hashPassword(password = credentials.password) ?: return@withContext null
        val newAccessToken = createAccessToken(userUuid = user.uuid, permissions = user.permissions) ?: return@withContext null
        val newRefreshToken = createRefreshToken() ?: return@withContext null
        val now = now()
        val uuid = uuid()

        database.safeTransaction {
            UserTable.upsert {
                it[UserTable.uuid] = user.uuid
                it[UserTable.modifiedAt] = user.modifiedAt
                it[UserTable.deletedAt] = user.deletedAt
                it[UserTable.permissions] = encode(value = user.permissions).orEmpty()
                it[UserTable.name] = user.name
                it[UserTable.avatar] = user.avatar
            }

            UserSecretTable.upsert {
                it[UserSecretTable.userUuid] = user.uuid
                it[UserSecretTable.username] = credentials.username
                it[UserSecretTable.passwordHash] = password
            }

            UserSessionTable.insert {
                it[UserSessionTable.uuid] = uuid
                it[UserSessionTable.userUuid] = user.uuid
                it[UserSessionTable.refreshToken] = newRefreshToken
                it[UserSessionTable.createdAt] = now
                it[UserSessionTable.expiresAt] = now + ServerConfigs.configs.refreshTokenValidity.milliseconds
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert user ${registrationForm.user.uuid}", throwable = it)
            return@withContext null
        }

        Authentication(
            user = user,
            bearer = BearerToken(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken
            )
        )
    }

    override suspend fun login(credentials: UserCredentials): Authentication? = withContext(context = Dispatcher.IO) {
        val row = database.safeTransaction {
            (UserSecretTable innerJoin UserTable)
                .selectAll()
                .where { UserSecretTable.username eq credentials.username }
                .firstOrNull()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get user ${credentials.username}", throwable = it)
        }.getOrNull() ?: return@withContext null

        val storedHash = runCatching { row[UserSecretTable.passwordHash] }.getOrNull() ?: return@withContext null
        if (!verifyPassword(password = credentials.password, hash = storedHash)) return@withContext null

        val user = runCatching { row.toUser() }.getOrNull() ?: return@withContext null
        val newAccessToken = createAccessToken(userUuid = user.uuid, permissions = user.permissions) ?: return@withContext null
        val newRefreshToken = createRefreshToken() ?: return@withContext null
        val now = now()
        val uuid = uuid()

        database.safeTransaction {
            UserSessionTable.insert {
                it[UserSessionTable.uuid] = uuid
                it[UserSessionTable.userUuid] = user.uuid
                it[UserSessionTable.refreshToken] = newRefreshToken
                it[UserSessionTable.createdAt] = now
                it[UserSessionTable.expiresAt] = now + ServerConfigs.configs.refreshTokenValidity.milliseconds
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to insert session for user ${credentials.username}", throwable = it)
            return@withContext null
        }

        Authentication(
            user = user,
            bearer = BearerToken(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken
            )
        )
    }

    override suspend fun logout(refreshToken: String): Uuid? = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            val userUuid = UserSessionTable.selectAll()
                .where { UserSessionTable.refreshToken eq refreshToken }
                .firstOrNull()?.get(expression = UserSessionTable.userUuid) ?: return@safeTransaction null
            UserSessionTable.deleteWhere { UserSessionTable.refreshToken eq refreshToken }
            userUuid
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to delete session", throwable = it)
        }.getOrNull()
    }

    override suspend fun refreshTokens(refreshToken: String): BearerToken? = withContext(context = Dispatcher.IO) {
        val now = now()

        val userUuid = database.safeTransaction {
            val sessionRow = UserSessionTable.selectAll()
                .where { UserSessionTable.refreshToken eq refreshToken }
                .firstOrNull() ?: return@safeTransaction null

            if (sessionRow[UserSessionTable.expiresAt] < now) {
                UserSessionTable.deleteWhere { UserSessionTable.refreshToken eq refreshToken }
                return@safeTransaction null
            }
            sessionRow[UserSessionTable.userUuid]
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get user uuid", throwable = it)
        }.getOrNull() ?: return@withContext null

        val permissions = database.safeTransaction {
            UserTable.selectAll()
                .where { UserTable.uuid eq userUuid }
                .firstOrNull()?.get(UserTable.permissions)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get permissions for $userUuid", throwable = it)
        }.getOrNull()?.let { decode<Map<EntityType, Permission>>(value = it) } ?: return@withContext null

        val newAccessToken = createAccessToken(userUuid = userUuid, permissions = permissions) ?: return@withContext null
        val newRefreshToken = createRefreshToken() ?: return@withContext null

        database.safeTransaction {
            UserSessionTable.update(where = { UserSessionTable.refreshToken eq refreshToken }) {
                it[UserSessionTable.refreshToken] = newRefreshToken
                it[UserSessionTable.expiresAt] = now + ServerConfigs.configs.refreshTokenValidity.milliseconds
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to update session", throwable = it)
            return@withContext null
        }

        BearerToken(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
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
        private const val TAG = "AuthenticationGateway"
    }
}