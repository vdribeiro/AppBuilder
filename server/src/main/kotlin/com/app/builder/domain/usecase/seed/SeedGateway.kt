package com.app.builder.domain.usecase.seed

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.select
import com.app.builder.core.flow.Dispatcher
import com.app.builder.core.locale.now
import com.app.builder.core.platform.Env
import com.app.builder.core.platform.readResource
import com.app.builder.core.security.hashPassword
import com.app.builder.core.security.toUuid
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.database.safeTransaction
import com.app.builder.data.database.table.BuildingEquipmentTable
import com.app.builder.data.database.table.BuildingMaterialTable
import com.app.builder.data.database.table.BuildingTable
import com.app.builder.data.database.table.BuildingUserTable
import com.app.builder.data.database.table.EquipmentTable
import com.app.builder.data.database.table.EquipmentUserTable
import com.app.builder.data.database.table.MaterialTable
import com.app.builder.data.database.table.RegistryTable
import com.app.builder.data.database.table.TaskTable
import com.app.builder.data.database.table.TranslationTable
import com.app.builder.data.database.table.UserAccountTable
import com.app.builder.data.database.table.UserTable
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.domain.Account
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.Translation
import com.app.builder.domain.User
import com.app.builder.domain.decodeTranslations
import com.app.builder.domain.usecase.Outcome
import com.app.builder.domain.usecase.account.AccountUseCases
import com.app.builder.domain.usecase.authentication.AuthenticationUseCases
import com.app.builder.domain.usecase.authentication.RegisterOutcome
import com.app.builder.domain.usecase.getOrNull

/**
 * Gateway implementation for seed use cases.
 *
 * @property database The database instance used to execute transactions.
 * @property authenticationUseCases Used to register the super user.
 * @property accountUseCases Used to restore the permissions of the super user.
 */
class SeedGateway(
    private val database: R2dbcDatabase,
    private val authenticationUseCases: AuthenticationUseCases,
    private val accountUseCases: AccountUseCases
): SeedUseCases {

    override suspend fun seed(): Outcome<Unit> {
        val uuid = Env.adminUuid?.toUuid()
        val username = Env.adminUsername
        val password = Env.adminPassword
        if (uuid == null || username.isNullOrBlank() || password.isNullOrBlank()) error(message = "Missing admin configurations")

        val outcomes = buildList {
            add(element = seedSuperUser(uuid = uuid, username = username, password = password))
            add(element = seedTranslationsResource())
            if (Env.developmentMode) add(element = seedEntitiesResource())
        }
        return if (outcomes.all { it is Outcome.Success }) Outcome.Success(data = Unit) else Outcome.Error
    }

    override suspend fun seedSuperUser(uuid: Uuid, username: String, password: String): Outcome<Unit> {
        val now = now()
        val superUser = User(
            uuid = uuid,
            modifiedAt = now,
            deletedAt = null,
            name = username,
            avatar = null
        )
        val superAccount = Account(
            userUuid = uuid,
            modifiedAt = now,
            deletedAt = null,
            username = username,
            permissions = EntityType.entries.associateWith { Permission.WRITE }
        )

        val result = authenticationUseCases.register(registrationForm = RegistrationForm(user = superUser, account = superAccount, password = password))
        if (result == RegisterOutcome.Success) {
            Telemetry.info(tag = TAG, message = "Su account created")
            return Outcome.Success(data = Unit)
        }

        val account = accountUseCases.getAccount(userUuid = uuid).getOrNull() ?: run {
            Telemetry.error(tag = TAG, message = "Su account not created")
            return Outcome.Error
        }

        if (account.permissions == superAccount.permissions) {
            Telemetry.info(tag = TAG, message = "Su account already exists")
            return Outcome.Success(data = Unit)
        }

        return when (accountUseCases.upsertAccount(account = account.copy(modifiedAt = now, permissions = superAccount.permissions))) {
            is Outcome.Success -> {
                Telemetry.info(tag = TAG, message = "Su account permissions restored")
                Outcome.Success(data = Unit)
            }
            else -> {
                Telemetry.error(tag = TAG, message = "Unable to restore su account permissions")
                Outcome.Error
            }
        }
    }

    override suspend fun seedTranslations(translations: List<Translation>): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        database.safeTransaction {
            val stored = TranslationTable
                .select(TranslationTable.languageIso, TranslationTable.key)
                .toList()
                .map { it[TranslationTable.languageIso] to it[TranslationTable.key] }
                .toSet()
            val missing = translations
                .distinctBy { it.languageIso to it.key }
                .filter { (it.languageIso to it.key) !in stored }
            TranslationTable.batchInsert(data = missing, shouldReturnGeneratedValues = false) { translation ->
                this[TranslationTable.languageIso] = translation.languageIso
                this[TranslationTable.key] = translation.key
                this[TranslationTable.modifiedAt] = translation.modifiedAt
                this[TranslationTable.value] = translation.value
            }
            Outcome.Success(data = Unit)
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to seed translations", throwable = it)
            Outcome.Error
        }
    }

    override suspend fun seedEntities(seed: Seed): Outcome<Unit> = withContext(context = Dispatcher.IO) {
        val passwordHashes = seed.registrations.associate { registration ->
            registration.account.userUuid to (hashPassword(password = registration.password) ?: return@withContext Outcome.Error)
        }

        database.safeTransaction {
            UserTable.insertMissing(uuid = UserTable.uuid, entities = seed.users + seed.registrations.map { it.user }, uuidOf = { it.uuid }) { user ->
                this[UserTable.uuid] = user.uuid
                this[UserTable.modifiedAt] = user.modifiedAt
                this[UserTable.deletedAt] = user.deletedAt
                this[UserTable.name] = user.name
                this[UserTable.avatar] = user.avatar
            }
            insertMissingAccounts(registrations = seed.registrations, passwordHashes = passwordHashes)
            TaskTable.insertMissing(uuid = TaskTable.uuid, entities = seed.tasks, uuidOf = { it.uuid }) { task ->
                this[TaskTable.uuid] = task.uuid
                this[TaskTable.modifiedAt] = task.modifiedAt
                this[TaskTable.deletedAt] = task.deletedAt
                this[TaskTable.title] = task.title
                this[TaskTable.description] = task.description
                this[TaskTable.state] = task.state.name
            }
            EquipmentTable.insertMissing(uuid = EquipmentTable.uuid, entities = seed.equipments, uuidOf = { it.uuid }) { equipment ->
                this[EquipmentTable.uuid] = equipment.uuid
                this[EquipmentTable.modifiedAt] = equipment.modifiedAt
                this[EquipmentTable.deletedAt] = equipment.deletedAt
                this[EquipmentTable.name] = equipment.name
                this[EquipmentTable.description] = equipment.description
                this[EquipmentTable.status] = equipment.status.name
            }
            BuildingTable.insertMissing(uuid = BuildingTable.uuid, entities = seed.buildings, uuidOf = { it.uuid }) { building ->
                this[BuildingTable.uuid] = building.uuid
                this[BuildingTable.modifiedAt] = building.modifiedAt
                this[BuildingTable.deletedAt] = building.deletedAt
                this[BuildingTable.name] = building.name
                this[BuildingTable.description] = building.description
                this[BuildingTable.address] = building.address
                this[BuildingTable.latitude] = building.latitude
                this[BuildingTable.longitude] = building.longitude
            }
            MaterialTable.insertMissing(uuid = MaterialTable.uuid, entities = seed.materials, uuidOf = { it.uuid }) { material ->
                this[MaterialTable.uuid] = material.uuid
                this[MaterialTable.modifiedAt] = material.modifiedAt
                this[MaterialTable.deletedAt] = material.deletedAt
                this[MaterialTable.name] = material.name
                this[MaterialTable.description] = material.description
            }
            BuildingEquipmentTable.insertMissing(uuid = BuildingEquipmentTable.uuid, entities = seed.buildingEquipments, uuidOf = { it.uuid }) { buildingEquipment ->
                this[BuildingEquipmentTable.uuid] = buildingEquipment.uuid
                this[BuildingEquipmentTable.modifiedAt] = buildingEquipment.modifiedAt
                this[BuildingEquipmentTable.deletedAt] = buildingEquipment.deletedAt
                this[BuildingEquipmentTable.buildingUuid] = buildingEquipment.buildingUuid
                this[BuildingEquipmentTable.equipmentUuid] = buildingEquipment.equipmentUuid
            }
            BuildingMaterialTable.insertMissing(uuid = BuildingMaterialTable.uuid, entities = seed.buildingMaterials, uuidOf = { it.uuid }) { buildingMaterial ->
                this[BuildingMaterialTable.uuid] = buildingMaterial.uuid
                this[BuildingMaterialTable.modifiedAt] = buildingMaterial.modifiedAt
                this[BuildingMaterialTable.deletedAt] = buildingMaterial.deletedAt
                this[BuildingMaterialTable.buildingUuid] = buildingMaterial.buildingUuid
                this[BuildingMaterialTable.materialUuid] = buildingMaterial.materialUuid
                this[BuildingMaterialTable.quantity] = buildingMaterial.quantity
            }
            BuildingUserTable.insertMissing(uuid = BuildingUserTable.uuid, entities = seed.buildingUsers, uuidOf = { it.uuid }) { buildingUser ->
                this[BuildingUserTable.uuid] = buildingUser.uuid
                this[BuildingUserTable.modifiedAt] = buildingUser.modifiedAt
                this[BuildingUserTable.deletedAt] = buildingUser.deletedAt
                this[BuildingUserTable.buildingUuid] = buildingUser.buildingUuid
                this[BuildingUserTable.userUuid] = buildingUser.userUuid
            }
            EquipmentUserTable.insertMissing(uuid = EquipmentUserTable.uuid, entities = seed.equipmentUsers, uuidOf = { it.uuid }) { equipmentUser ->
                this[EquipmentUserTable.uuid] = equipmentUser.uuid
                this[EquipmentUserTable.modifiedAt] = equipmentUser.modifiedAt
                this[EquipmentUserTable.deletedAt] = equipmentUser.deletedAt
                this[EquipmentUserTable.equipmentUuid] = equipmentUser.equipmentUuid
                this[EquipmentUserTable.userUuid] = equipmentUser.userUuid
            }
            RegistryTable.insertMissing(uuid = RegistryTable.requestUuid, entities = seed.registries, uuidOf = { it.requestUuid }) { registry ->
                this[RegistryTable.requestUuid] = registry.requestUuid
                this[RegistryTable.requestType] = registry.requestType
                this[RegistryTable.requestUtc] = registry.requestUtc
                this[RegistryTable.requestSentUtc] = registry.requestSentUtc
                this[RegistryTable.userUuid] = registry.userUuid
                this[RegistryTable.appVersion] = registry.appVersion
                this[RegistryTable.os] = registry.os.toString()
                this[RegistryTable.osVersion] = registry.osVersion
                this[RegistryTable.brand] = registry.brand
                this[RegistryTable.model] = registry.model
                this[RegistryTable.deviceUuid] = registry.deviceUuid
                this[RegistryTable.entityUuid] = registry.entityUuid
                this[RegistryTable.entityType] = registry.entityType?.name
                this[RegistryTable.payload] = registry.payload
            }
            Outcome.Success(data = Unit)
        }.getOrElse {
            Telemetry.error(tag = TAG, message = "Unable to seed entities", throwable = it)
            Outcome.Error
        }
    }

    /**
     * Seeds the translations of the [TRANSLATIONS_RESOURCE] file.
     *
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the file can't be loaded or the operation failed.
     */
    private suspend fun seedTranslationsResource(): Outcome<Unit> {
        val translations = readResource(path = TRANSLATIONS_RESOURCE)
            ?.let { decodeTranslations(value = it) }
            ?: run {
                Telemetry.error(tag = TAG, message = "Unable to load the translations file")
                return Outcome.Error
            }

        return seedTranslations(translations = translations).also {
            if (it is Outcome.Success) Telemetry.info(tag = TAG, message = "Translations seeded")
        }
    }

    /**
     * Seeds the entities of the [SEED_RESOURCE] file.
     *
     * @return [Outcome.Success] when persisted, or [Outcome.Error] when the file can't be loaded or the operation failed.
     */
    private suspend fun seedEntitiesResource(): Outcome<Unit> {
        val seed = readResource(path = SEED_RESOURCE)
            ?.let { decode<Seed>(value = it) }
            ?: run {
                Telemetry.error(tag = TAG, message = "Unable to load the seed file")
                return Outcome.Error
            }

        return seedEntities(seed = seed).also {
            if (it is Outcome.Success) Telemetry.info(tag = TAG, message = "Entities seeded")
        }
    }

    /**
     * Inserts the accounts whose user has no account and whose username is not stored yet.
     *
     * @param registrations The registrations holding the accounts to insert.
     * @param passwordHashes The hashed password of each account, by the uuid of its user.
     */
    private suspend fun insertMissingAccounts(registrations: List<RegistrationForm>, passwordHashes: Map<Uuid, String>) {
        val stored = UserAccountTable
            .select(UserAccountTable.userUuid, UserAccountTable.username)
            .toList()
        val storedUserUuids = stored.map { it[UserAccountTable.userUuid] }.toSet()
        val storedUsernames = stored.map { it[UserAccountTable.username] }.toSet()
        val missing = registrations
            .map { it.account }
            .distinctBy { it.userUuid }
            .distinctBy { it.username }
            .filter { it.userUuid !in storedUserUuids && it.username !in storedUsernames }
        UserAccountTable.batchInsert(data = missing, shouldReturnGeneratedValues = false) { account ->
            this[UserAccountTable.userUuid] = account.userUuid
            this[UserAccountTable.modifiedAt] = account.modifiedAt
            this[UserAccountTable.deletedAt] = account.deletedAt
            this[UserAccountTable.username] = account.username
            this[UserAccountTable.passwordHash] = passwordHashes.getValue(key = account.userUuid)
            this[UserAccountTable.permissions] = encode(value = account.permissions).orEmpty()
        }
    }

    /**
     * Inserts the entities whose uuid is not stored in this table yet, keeping the first of the entities that share a uuid.
     *
     * @receiver The table to insert into.
     * @param uuid The column identifying the rows of the table.
     * @param entities The entities to insert.
     * @param uuidOf Gets the uuid of an entity.
     * @param body Sets the columns of the row of an entity.
     */
    private suspend fun <E> Table.insertMissing(
        uuid: Column<Uuid>,
        entities: List<E>,
        uuidOf: (E) -> Uuid,
        body: BatchInsertStatement.(E) -> Unit
    ) {
        val stored = select(uuid)
            .toList()
            .map { it[uuid] }
            .toSet()
        val missing = entities
            .distinctBy(selector = uuidOf)
            .filter { uuidOf(it) !in stored }
        batchInsert(data = missing, shouldReturnGeneratedValues = false, body = body)
    }

    companion object {
        private const val TAG = "SeedGateway"

        /** Classpath location of the translations seeded into the database. */
        private const val TRANSLATIONS_RESOURCE = "static/translations.json"

        /** Classpath location of the entities seeded into the database in development mode. */
        private const val SEED_RESOURCE = "static/seed.json"
    }
}
