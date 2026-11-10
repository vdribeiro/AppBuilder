package com.app.builder.domain.usecase.authentication

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
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
import com.app.builder.core.security.hashRefreshToken
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
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.User
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.permission.UserAccess
import com.app.builder.domain.permission.createAccessToken
import com.app.builder.domain.usecase.Outcome

/**
 * Gateway implementation for authentication use cases.
 *
 * @property database The database instance used to execute transactions.
 */
class AuthenticationGateway(
    private val database: R2dbcDatabase
): AuthenticationUseCases {

    override suspend fun register(registrationForm: RegistrationForm): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val user = registrationForm.user
        val account = registrationForm.account
        if (account.userUuid != user.uuid) {
            Telemetry.error(tag = TAG, message = "Rejected registration for user ${user.uuid} with an account of user ${account.userUuid}")
            return@withContext Outcome.Invalid
        }
        val password = hashPassword(password = registrationForm.password) ?: return@withContext Outcome.Error

        database.safeTransaction {
            val accountExists = UserAccountTable.selectAll()
                .where { (UserAccountTable.userUuid eq user.uuid) or (UserAccountTable.username eq account.username) }
                .firstOrNull() != null
            if (accountExists) {
                Telemetry.error(tag = TAG, message = "Rejected registration for user ${user.uuid}, the user already has an account or the username is taken")
                return@safeTransaction Outcome.Conflict
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
            Outcome.Success(data = Unit)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to register an account for user ${user.uuid}", throwable = it)
        }.getOrDefault(defaultValue = Outcome.Error)
    }

    override suspend fun login(credentials: UserCredentials): Outcome<Authentication> = withContext(context = Dispatcher.IO) {
        val row = database.safeTransaction {
            (UserAccountTable innerJoin UserTable)
                .selectAll()
                .where { UserAccountTable.username eq credentials.username }
                .firstOrNull()
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get account", throwable = it)
            return@withContext Outcome.Error
        } ?: return@withContext Outcome.Rejected

        val storedHash = runCatching { row[UserAccountTable.passwordHash] }.getOrNull() ?: return@withContext Outcome.Error
        if (!verifyPassword(password = credentials.password, hash = storedHash)) return@withContext Outcome.Rejected

        val user = runCatching { row.toUser() }.getOrNull() ?: return@withContext Outcome.Error
        val account = runCatching { row.toAccount() }.getOrNull() ?: return@withContext Outcome.Error
        if (account.deletedAt != null) {
            Telemetry.error(tag = TAG, message = "Rejected login for disabled account of user ${user.uuid}")
            return@withContext Outcome.Rejected
        }

        val newAccessToken = createAccessToken(userUuid = user.uuid, access = UserAccess(permissions = account.permissions, deletedAt = null)) ?: return@withContext Outcome.Error
        val newRefreshToken = createRefreshToken() ?: return@withContext Outcome.Error
        val now = now()
        val uuid = uuid()

        database.safeTransaction {
            UserSessionTable.insert {
                it[UserSessionTable.uuid] = uuid
                it[UserSessionTable.userUuid] = user.uuid
                it[UserSessionTable.refreshToken] = hashRefreshToken(refreshToken = newRefreshToken)
                it[UserSessionTable.createdAt] = now
                it[UserSessionTable.expiresAt] = now + ServerConfigs.value.refreshTokenValidity.milliseconds
            }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to insert session", throwable = it)
            return@withContext Outcome.Error
        }

        Outcome.Success(
            data = Authentication(
                user = user,
                account = account,
                bearer = BearerToken(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken
                )
            )
        )
    }

    override suspend fun logout(refreshToken: String): Outcome<Uuid> = withContext(context = Dispatcher.IO) {
        val now = now()
        database.safeTransaction {
            val session = findSession(refreshToken = refreshToken, now = now) ?: return@safeTransaction null
            UserSessionTable.deleteWhere { UserSessionTable.uuid eq session[UserSessionTable.uuid] }
            session[UserSessionTable.userUuid]
        }.fold(
            onSuccess = { userUuid -> userUuid?.let { Outcome.Success(data = it) } ?: Outcome.Rejected },
            onFailure = {
                Telemetry.error(tag = TAG, message = "Unable to delete session", throwable = it)
                Outcome.Error
            }
        )
    }

    override suspend fun refreshTokens(refreshToken: String): Outcome<BearerToken> = withContext(context = Dispatcher.IO) {
        val now = now()

        val session = database.safeTransaction {
            findSession(refreshToken = refreshToken, now = now)
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get session", throwable = it)
            return@withContext Outcome.Error
        } ?: return@withContext Outcome.Rejected

        val sessionUuid = session[UserSessionTable.uuid]
        if (session[UserSessionTable.expiresAt] < now) {
            database.safeTransaction {
                UserSessionTable.deleteWhere { UserSessionTable.uuid eq sessionUuid }
            }.onFailure {
                Telemetry.error(tag = TAG, message = "Unable to delete expired session", throwable = it)
            }
            return@withContext Outcome.Rejected
        }
        val userUuid = session[UserSessionTable.userUuid]

        val accountRow = database.safeTransaction {
            UserAccountTable.selectAll()
                .where { UserAccountTable.userUuid eq userUuid }
                .firstOrNull()
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get the account of user $userUuid", throwable = it)
            return@withContext Outcome.Error
        } ?: return@withContext Outcome.Rejected

        val access = UserAccess(
            permissions = decode<Map<EntityType, Permission>>(value = accountRow[UserAccountTable.permissions]) ?: return@withContext Outcome.Error,
            deletedAt = accountRow[UserAccountTable.deletedAt]
        )
        val newAccessToken = createAccessToken(userUuid = userUuid, access = access) ?: return@withContext Outcome.Error

        val presentedHash = hashRefreshToken(refreshToken = refreshToken)
        val currentHash = session[UserSessionTable.refreshToken]
        val replayed = currentHash != presentedHash
        if (replayed) Telemetry.info(tag = TAG, message = "Refreshing session $sessionUuid with its previous refresh token")

        val newRefreshToken = createRefreshToken() ?: return@withContext Outcome.Error
        val rotated = database.safeTransaction {
            UserSessionTable.update(where = { (UserSessionTable.uuid eq sessionUuid) and (UserSessionTable.refreshToken eq currentHash) }) {
                it[UserSessionTable.refreshToken] = hashRefreshToken(refreshToken = newRefreshToken)
                if (!replayed) {
                    it[UserSessionTable.previousRefreshToken] = presentedHash
                    it[UserSessionTable.rotatedAt] = now
                }
                it[UserSessionTable.expiresAt] = now + ServerConfigs.value.refreshTokenValidity.milliseconds
            } > 0
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to update session", throwable = it)
            return@withContext Outcome.Error
        }
        if (!rotated) {
            Telemetry.info(tag = TAG, message = "Session $sessionUuid was rotated concurrently, refreshing it again")
            return@withContext refreshTokens(refreshToken = refreshToken)
        }

        Outcome.Success(
            data = BearerToken(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken
            )
        )
    }

    /**
     * Finds the session [refreshToken] belongs to, as its current refresh token or as the previous one within [ServerConfigs.refreshTokenGracePeriod] of its rotation.
     * The last operation is so that clients that miss the rotated pair can still recover.
     *
     * @param refreshToken The refresh token presented by the client.
     * @param now The time the request is handled at.
     * @return The session row, or `null` if no session accepts [refreshToken].
     */
    private suspend fun findSession(refreshToken: String, now: Instant): ResultRow? {
        val hash = hashRefreshToken(refreshToken = refreshToken)
        return UserSessionTable.selectAll()
            .where { UserSessionTable.refreshToken eq hash }
            .firstOrNull()
            ?: UserSessionTable.selectAll()
                .where { UserSessionTable.previousRefreshToken eq hash }
                .firstOrNull()
                ?.takeIf { session -> session[UserSessionTable.rotatedAt]?.let { now < it + ServerConfigs.value.refreshTokenGracePeriod.milliseconds } == true }
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