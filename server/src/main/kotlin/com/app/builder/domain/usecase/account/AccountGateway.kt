package com.app.builder.domain.usecase.account

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.DeviceTokenTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.usecase.WriteResult

/**
 * Gateway implementation for accounts use cases. Password hashes are never read.
 *
 * @property database The database instance used to execute transactions.
 */
class AccountGateway(
    private val database: R2dbcDatabase,
): AccountUseCases {

    override suspend fun getAccount(userUuid: Uuid): Account? = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            UserAccountTable
                .select(columns = accountColumns)
                .where { UserAccountTable.userUuid eq userUuid }
                .firstOrNull()
                ?.toAccount()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to get the account of user $userUuid", throwable = it)
        }.getOrNull()
    }

    override suspend fun upsertAccount(account: Account): WriteResult = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            val owners = UserAccountTable.select(UserAccountTable.userUuid)
                .where { (UserAccountTable.userUuid eq account.userUuid) or (UserAccountTable.username eq account.username) }
                .toList()
                .map { it[UserAccountTable.userUuid] }
            if (account.userUuid !in owners) return@safeTransaction WriteResult.NOT_FOUND
            if (owners.any { it != account.userUuid }) {
                Telemetry.error(tag = TAG, message = "Rejected the account of user ${account.userUuid}, the username is taken")
                return@safeTransaction WriteResult.CONFLICT
            }

            UserAccountTable.update(where = { UserAccountTable.userUuid eq account.userUuid }) {
                it[UserAccountTable.modifiedAt] = account.modifiedAt
                it[UserAccountTable.deletedAt] = account.deletedAt
                it[UserAccountTable.username] = account.username
                it[UserAccountTable.permissions] = encode(value = account.permissions).orEmpty()
            }
            if (account.deletedAt != null) DeviceTokenTable.deleteWhere { DeviceTokenTable.userUuid eq account.userUuid }
            WriteResult.SUCCESS
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to upsert the account of user ${account.userUuid}", throwable = it)
        }.getOrDefault(defaultValue = WriteResult.ERROR)
    }

    /**
     * Maps a database row to an [Account] domain model.
     *
     * @receiver A row from [UserAccountTable] holding the [accountColumns].
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
        private const val TAG = "AccountGateway"

        /** The [UserAccountTable] columns read into an [Account], leaving out the password hash. */
        private val accountColumns = listOf(
            UserAccountTable.userUuid,
            UserAccountTable.modifiedAt,
            UserAccountTable.deletedAt,
            UserAccountTable.username,
            UserAccountTable.permissions
        )
    }
}
