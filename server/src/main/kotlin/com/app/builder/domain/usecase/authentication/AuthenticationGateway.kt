package com.app.builder.domain.usecase.authentication

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.plus
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
import com.app.builder.core.security.hashRefreshToken
import com.app.builder.core.security.uuid
import com.app.builder.core.security.verifyPassword
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.LoginAttemptTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.database.table.UserSessionTable
import com.app.builder.data.database.table.UserTable
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
import com.app.builder.domain.Authentication
import com.app.builder.domain.BearerToken
import com.app.builder.domain.EntityType
import com.app.builder.domain.PasswordChange
import com.app.builder.domain.PasswordFailure
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

    override suspend fun register(registrationForm: RegistrationForm): RegisterOutcome = withContext(context = Dispatcher.IO) {
        val user = registrationForm.user
        val account = registrationForm.account
        if (account.userUuid != user.uuid) {
            Telemetry.error(tag = TAG, message = "Rejected registration for user ${user.uuid} with an account of user ${account.userUuid}")
            return@withContext RegisterOutcome.Invalid
        }
        validatePassword(password = registrationForm.password)?.let {
            Telemetry.error(tag = TAG, message = "Rejected registration for user ${user.uuid} with an invalid password")
            return@withContext RegisterOutcome.InvalidPassword(failure = it)
        }
        val password = hashPassword(password = registrationForm.password) ?: return@withContext RegisterOutcome.Error

        database.safeTransaction {
            val accountExists = UserAccountTable.selectAll()
                .where { (UserAccountTable.userUuid eq user.uuid) or (UserAccountTable.username eq account.username) }
                .firstOrNull() != null
            if (accountExists) {
                Telemetry.error(tag = TAG, message = "Rejected registration for user ${user.uuid}, the user already has an account or the username is taken")
                return@safeTransaction RegisterOutcome.Conflict
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
            RegisterOutcome.Success
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to register an account for user ${user.uuid}", throwable = it)
        }.getOrDefault(defaultValue = RegisterOutcome.Error)
    }

    override suspend fun login(credentials: UserCredentials): LoginOutcome = withContext(context = Dispatcher.IO) {
        val now = now()
        val locked = database.safeTransaction {
            LoginAttemptTable.selectAll()
                .where { LoginAttemptTable.username eq credentials.username }
                .firstOrNull()
                ?.get(LoginAttemptTable.lockedAt)
                ?.let { now < it + ServerConfigs.value.loginLockDuration.milliseconds } == true
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get login attempts", throwable = it)
            return@withContext LoginOutcome.Error
        }
        if (locked) {
            Telemetry.error(tag = TAG, message = "Rejected login for locked username")
            return@withContext LoginOutcome.Locked
        }

        val row = database.safeTransaction {
            (UserAccountTable innerJoin UserTable)
                .selectAll()
                .where { UserAccountTable.username eq credentials.username }
                .firstOrNull()
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get account", throwable = it)
            return@withContext LoginOutcome.Error
        }

        val storedHash = row?.let { runCatching { it[UserAccountTable.passwordHash] }.getOrNull() ?: return@withContext LoginOutcome.Error }
        if (!verifyPassword(password = credentials.password, hash = storedHash) || row == null) {
            recordLoginFailure(username = credentials.username, now = now)
            return@withContext LoginOutcome.Rejected
        }
        clearLoginFailures(username = credentials.username)

        val user = runCatching { row.toUser() }.getOrNull() ?: return@withContext LoginOutcome.Error
        val account = runCatching { row.toAccount() }.getOrNull() ?: return@withContext LoginOutcome.Error
        if (account.deletedAt != null) {
            Telemetry.error(tag = TAG, message = "Rejected login for disabled account of user ${user.uuid}")
            return@withContext LoginOutcome.Disabled
        }

        val newAccessToken = createAccessToken(userUuid = user.uuid, access = UserAccess(permissions = account.permissions, deletedAt = null)) ?: return@withContext LoginOutcome.Error
        val newRefreshToken = createRefreshToken() ?: return@withContext LoginOutcome.Error
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
            return@withContext LoginOutcome.Error
        }

        LoginOutcome.Success(
            authentication = Authentication(
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
        val expiresAt = listOfNotNull(
            now + ServerConfigs.value.refreshTokenValidity.milliseconds,
            access.deletedAt?.plus(ServerConfigs.value.deletedAccountSessionValidity.milliseconds)
        ).min()
        if (expiresAt < now) {
            database.safeTransaction {
                UserSessionTable.deleteWhere { UserSessionTable.uuid eq sessionUuid }
            }.onFailure {
                Telemetry.error(tag = TAG, message = "Unable to delete the session of disabled user $userUuid", throwable = it)
            }
            return@withContext Outcome.Rejected
        }
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
                it[UserSessionTable.expiresAt] = expiresAt
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

    override suspend fun changePassword(passwordChange: PasswordChange): PasswordChangeOutcome = withContext(context = Dispatcher.IO) {
        val now = now()

        val (session, account) = database.safeTransaction {
            val session = findSession(refreshToken = passwordChange.refreshToken, now = now)
                ?.takeIf { it[UserSessionTable.expiresAt] >= now }
                ?: return@safeTransaction null
            val account = UserAccountTable.selectAll()
                .where { UserAccountTable.userUuid eq session[UserSessionTable.userUuid] }
                .firstOrNull()
                ?: return@safeTransaction null
            session to account
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to get session and account", throwable = it)
            return@withContext PasswordChangeOutcome.Error
        } ?: return@withContext PasswordChangeOutcome.Rejected

        val sessionUuid = session[UserSessionTable.uuid]
        val userUuid = session[UserSessionTable.userUuid]
        if (account[UserAccountTable.deletedAt] != null) {
            Telemetry.error(tag = TAG, message = "Rejected password change for disabled account of user $userUuid")
            return@withContext PasswordChangeOutcome.Rejected
        }
        validatePassword(password = passwordChange.newPassword)?.let { return@withContext PasswordChangeOutcome.InvalidPassword(failure = it) }

        val maxAttempts = ServerConfigs.value.maxPasswordAttempts.coerceAtLeast(minimumValue = 1)
        val counted = database.safeTransaction {
            val counted = UserSessionTable.update(where = { (UserSessionTable.uuid eq sessionUuid) and (UserSessionTable.passwordAttempts less maxAttempts) }) {
                it[UserSessionTable.passwordAttempts] = UserSessionTable.passwordAttempts + 1
            } > 0
            if (!counted) UserSessionTable.deleteWhere { UserSessionTable.uuid eq sessionUuid }
            counted
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to count the password attempt of session $sessionUuid", throwable = it)
            return@withContext PasswordChangeOutcome.Error
        }
        if (!counted) return@withContext PasswordChangeOutcome.Rejected

        if (!verifyPassword(password = passwordChange.currentPassword, hash = account[UserAccountTable.passwordHash])) {
            val ended = database.safeTransaction {
                UserSessionTable.deleteWhere { (UserSessionTable.uuid eq sessionUuid) and (UserSessionTable.passwordAttempts greaterEq maxAttempts) } > 0
            }.getOrElse {
                Telemetry.error(tag = TAG, message = "Unable to end session $sessionUuid", throwable = it)
                return@withContext PasswordChangeOutcome.Error
            }
            if (!ended) {
                val remainingAttempts = (maxAttempts - session[UserSessionTable.passwordAttempts] - 1).coerceAtLeast(minimumValue = 1)
                return@withContext PasswordChangeOutcome.InvalidPassword(failure = PasswordFailure.WrongPassword(remainingAttempts = remainingAttempts))
            }
            Telemetry.error(tag = TAG, message = "Ended session $sessionUuid of user $userUuid after $maxAttempts wrong current passwords")
            return@withContext PasswordChangeOutcome.Rejected
        }
        val password = hashPassword(password = passwordChange.newPassword) ?: return@withContext PasswordChangeOutcome.Error

        database.safeTransaction {
            UserAccountTable.update(where = { UserAccountTable.userUuid eq userUuid }) {
                it[UserAccountTable.passwordHash] = password
            }
            UserSessionTable.update(where = { UserSessionTable.uuid eq sessionUuid }) {
                it[UserSessionTable.passwordAttempts] = 0
            }
            UserSessionTable.deleteWhere { (UserSessionTable.userUuid eq userUuid) and (UserSessionTable.uuid neq sessionUuid) }
            PasswordChangeOutcome.Success(userUuid = userUuid)
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to change the password of user $userUuid", throwable = it)
        }.getOrDefault(defaultValue = PasswordChangeOutcome.Error)
    }

    /**
     * Counts a failed login of [username], starting the count over when the last failure is older than [ServerConfigs.loginLockDuration],
     * and locks the username for [ServerConfigs.loginLockDuration] once the count reaches [ServerConfigs.maxLoginAttempts].
     * Deletes the counts of every username whose last failure and lock are both older than [ServerConfigs.loginLockDuration].
     *
     * @param username The username of the failed login.
     * @param now The time the login is handled at.
     */
    private suspend fun recordLoginFailure(username: String, now: Instant) {
        val lockDuration = ServerConfigs.value.loginLockDuration.milliseconds
        val maxAttempts = ServerConfigs.value.maxLoginAttempts.coerceAtLeast(minimumValue = 1)
        database.safeTransaction {
            val expired = now - lockDuration
            LoginAttemptTable.deleteWhere {
                (LoginAttemptTable.lastFailureAt less expired) and (LoginAttemptTable.lockedAt.isNull() or (LoginAttemptTable.lockedAt less expired))
            }
            val previous = LoginAttemptTable.selectAll()
                .where { LoginAttemptTable.username eq username }
                .firstOrNull()
                ?.takeIf { it[LoginAttemptTable.lastFailureAt] >= expired }
                ?.get(LoginAttemptTable.failures)
                ?: 0
            val failures = previous + 1
            LoginAttemptTable.upsert {
                it[LoginAttemptTable.username] = username
                it[LoginAttemptTable.failures] = if (failures >= maxAttempts) 0 else failures
                it[LoginAttemptTable.lastFailureAt] = now
                it[LoginAttemptTable.lockedAt] = if (failures >= maxAttempts) now else null
            }
            if (failures >= maxAttempts) Telemetry.error(tag = TAG, message = "Locked a username after $maxAttempts failed logins")
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to count a failed login", throwable = it)
        }
    }

    /**
     * Deletes the count of failed logins of [username].
     *
     * @param username The username of the successful login.
     */
    private suspend fun clearLoginFailures(username: String) {
        database.safeTransaction {
            LoginAttemptTable.deleteWhere { LoginAttemptTable.username eq username }
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to clear the failed logins", throwable = it)
        }
    }

    /**
     * Checks if [password] can be set as the password of an account: not blank, at least [ServerConfigs.minPasswordLength] characters and at most [MAX_PASSWORD_BYTES] bytes.
     *
     * @param password The password to check.
     * @return Why the password can't be set, or `null` if it can.
     */
    private fun validatePassword(password: String): PasswordFailure? {
        val minLength = ServerConfigs.value.minPasswordLength
        return when {
            password.isBlank() || password.length < minLength -> PasswordFailure.TooShort(minLength = minLength)
            password.encodeToByteArray().size > MAX_PASSWORD_BYTES -> PasswordFailure.TooLong
            else -> null
        }
    }

    /**
     * Finds the session [refreshToken] belongs to, as its current refresh token or as the previous one within [ServerConfigs.refreshTokenGracePeriod] of its rotation.
     * The last operation is so that clients that miss the rotated pair can still recover.
     * A previous refresh token presented after that period deletes its session.
     *
     * @param refreshToken The refresh token presented by the client.
     * @param now The time the request is handled at.
     * @return The session row, or `null` if no session accepts [refreshToken].
     */
    private suspend fun findSession(refreshToken: String, now: Instant): ResultRow? {
        val hash = hashRefreshToken(refreshToken = refreshToken)
        UserSessionTable.selectAll()
            .where { UserSessionTable.refreshToken eq hash }
            .firstOrNull()
            ?.let { return it }

        val session = UserSessionTable.selectAll()
            .where { UserSessionTable.previousRefreshToken eq hash }
            .firstOrNull()
            ?: return null

        if (session[UserSessionTable.rotatedAt]?.let { now < it + ServerConfigs.value.refreshTokenGracePeriod.milliseconds } == true) return session

        val sessionUuid = session[UserSessionTable.uuid]
        UserSessionTable.deleteWhere { UserSessionTable.uuid eq sessionUuid }
        Telemetry.error(tag = TAG, message = "Deleted session $sessionUuid of user ${session[UserSessionTable.userUuid]}: previous refresh token reused after the grace period")
        return null
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

        /** The longest password the hash takes into account, in bytes. */
        private const val MAX_PASSWORD_BYTES = 72
    }
}