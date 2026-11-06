package com.app.builder.domain.usecase.authentication

import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import com.app.builder.core.config.ServerConfigs
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.security.createRefreshToken
import com.app.builder.core.security.hashPassword
import com.app.builder.core.security.uuid
import com.app.builder.core.security.verifyPassword
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.database.table.UserSessionTable
import com.app.builder.data.database.table.UserTable
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
import com.app.builder.domain.usecase.WriteResult
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.permission.createAccessToken

/**
 * Gateway implementation for authentication use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class AuthenticationGateway(
    private val database: R2dbcDatabase
): AuthenticationUseCases {

    override suspend fun register(registrationForm: RegistrationForm): WriteResult = withContext(context = Dispatcher.IO) {
        val user = registrationForm.user
        val account = registrationForm.account
        if (account.userUuid != user.uuid) {
            Telemetry.error(tag = TAG, message = "Rejected registration for user ${user.uuid} with an account of user ${account.userUuid}")
            return@withContext WriteResult.INVALID
        }
        val password = hashPassword(password = registrationForm.password) ?: return@withContext WriteResult.ERROR

        database.safeTransaction {
            val accountExists = UserAccountTable.selectAll()
                .where { (UserAccountTable.userUuid eq user.uuid) or (UserAccountTable.username eq account.username) }
                .firstOrNull() != null
            if (accountExists) {
                Telemetry.error(tag = TAG, message = "Rejected registration for user ${user.uuid}, the user already has an account or the username is taken")
                return@safeTransaction WriteResult.CONFLICT
            }

            val userExists = UserTable.selectAll()
                .where { UserTable.uuid eq user.uuid }
                .firstOrNull() != null
            if (!userExists) {
                UserTable.insert {
                    it[UserTable.uuid] = user.uuid
                    it[UserTable.modifiedAt] = user.modifiedAt
                    it[UserTable.deletedAt] = user.deletedAt
                    it[UserTable.name] = user.name
                    it[UserTable.avatar] = user.avatar
                }
            }

            UserAccountTable.insert {
                it[UserAccountTable.userUuid] = account.userUuid
                it[UserAccountTable.modifiedAt] = account.modifiedAt
                it[UserAccountTable.deletedAt] = account.deletedAt
                it[UserAccountTable.username] = account.username
                it[UserAccountTable.passwordHash] = password
                it[UserAccountTable.permissions] = encode(value = account.permissions).orEmpty()
            }
            WriteResult.SUCCESS
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to register an account for user ${user.uuid}", throwable = it)
        }.getOrDefault(defaultValue = WriteResult.ERROR)
    }

    override suspend fun login(credentials: UserCredentials): Authentication? = withContext(context = Dispatcher.IO) {
        val row = database.safeTransaction {
            (UserAccountTable innerJoin UserTable)
                .selectAll()
                .where { UserAccountTable.username eq credentials.username }
                .firstOrNull()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get account", throwable = it)
        }.getOrNull() ?: return@withContext null

        val storedHash = runCatching { row[UserAccountTable.passwordHash] }.getOrNull() ?: return@withContext null
        if (!verifyPassword(password = credentials.password, hash = storedHash)) return@withContext null

        val user = runCatching { row.toUser() }.getOrNull() ?: return@withContext null
        val account = runCatching { row.toAccount() }.getOrNull() ?: return@withContext null
        if (account.deletedAt != null) {
            Telemetry.error(tag = TAG, message = "Rejected login for disabled account of user ${user.uuid}")
            return@withContext null
        }

        val newAccessToken = createAccessToken(userUuid = user.uuid, access = UserAccess(permissions = account.permissions, deletedAt = null)) ?: return@withContext null
        val newRefreshToken = createRefreshToken() ?: return@withContext null
        val now = now()
        val uuid = uuid()

        database.safeTransaction {
            UserSessionTable.insert {
                it[UserSessionTable.uuid] = uuid
                it[UserSessionTable.userUuid] = user.uuid
                it[UserSessionTable.refreshToken] = newRefreshToken
                it[UserSessionTable.createdAt] = now
                it[UserSessionTable.expiresAt] = now + ServerConfigs.value.refreshTokenValidity.milliseconds
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to insert session", throwable = it)
            return@withContext null
        }

        Authentication(
            user = user,
            account = account,
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

        val accountRow = database.safeTransaction {
            UserAccountTable.selectAll()
                .where { UserAccountTable.userUuid eq userUuid }
                .firstOrNull()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get the account of user $userUuid", throwable = it)
        }.getOrNull() ?: return@withContext null

        val access = UserAccess(
            permissions = decode<Map<EntityType, Permission>>(value = accountRow[UserAccountTable.permissions]) ?: return@withContext null,
            deletedAt = accountRow[UserAccountTable.deletedAt]
        )

        val newAccessToken = createAccessToken(userUuid = userUuid, access = access) ?: return@withContext null
        val newRefreshToken = createRefreshToken() ?: return@withContext null

        database.safeTransaction {
            UserSessionTable.update(where = { UserSessionTable.refreshToken eq refreshToken }) {
                it[UserSessionTable.refreshToken] = newRefreshToken
                it[UserSessionTable.expiresAt] = now + ServerConfigs.value.refreshTokenValidity.milliseconds
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
        name = this[UserTable.name],
        avatar = this[UserTable.avatar]
    )

    /**
     * Maps a database row to an [Account] domain model.
     *
     * @receiver A row from [UserAccountTable].
     * @return The mapped [Account].
     */
    private fun ResultRow.toAccount(): Account = Account(
        userUuid = this[UserAccountTable.userUuid],
        modifiedAt = this[UserAccountTable.modifiedAt],
        deletedAt = this[UserAccountTable.deletedAt],
        username = this[UserAccountTable.username],
        permissions = decode<Map<EntityType, Permission>>(value = this[UserAccountTable.permissions]).orEmpty()
    )

    companion object {
        private const val TAG = "AuthenticationGateway"
    }
}