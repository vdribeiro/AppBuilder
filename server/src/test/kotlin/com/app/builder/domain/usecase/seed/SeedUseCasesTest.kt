package com.app.builder.domain.usecase.seed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import com.app.builder.core.locale.now
import com.app.builder.core.security.uuid
import com.app.builder.data.database.reset
import com.app.builder.domain.EntityType
import com.app.builder.domain.Permission
import com.app.builder.domain.RegistrationForm
import com.app.builder.domain.UserCredentials
import com.app.builder.domain.usecase.Outcome
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class SeedUseCasesTest: TestCase() {

    /** A user without an account. */
    private val seedUser = FakeData.user.copy(uuid = uuid(), name = "Seed User")

    /** A user with an account. */
    private val seedRegistration: RegistrationForm = registrationForm(username = "seed", permissions = mapOf(EntityType.TASK to Permission.READ))

    /** A seed holding one entity of each kind. */
    private val seed = Seed(
        users = listOf(element = seedUser),
        registrations = listOf(element = seedRegistration),
        tasks = listOf(element = FakeData.task),
        equipments = listOf(element = FakeData.equipment),
        buildings = listOf(element = FakeData.building),
        materials = listOf(element = FakeData.material),
        buildingEquipments = listOf(element = FakeData.buildingEquipment),
        buildingMaterials = listOf(element = FakeData.buildingMaterial),
        buildingUsers = listOf(element = FakeData.buildingUser),
        equipmentUsers = listOf(element = FakeData.equipmentUser),
        registries = listOf(element = FakeData.registry)
    )

    /** Verifies that seeding the super user registers an account with write permission on every entity type that can log in. */
    @Test
    fun seedSuperUser() = runServerTest {
        val useCases = dependency.get().useCases
        val uuid = uuid()

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedSuperUser(uuid = uuid, username = "su", password = "su"))

        val account = useCases.accountUseCases.getAccount(userUuid = uuid).successData()
        assertEquals(expected = "su", actual = account.username)
        assertEquals(expected = EntityType.entries.associateWith { Permission.WRITE }, actual = account.permissions)
        assertEquals(expected = "su", actual = useCases.userUseCases.getUser(uuid = uuid).successData().name)
        assertEquals(expected = account, actual = useCases.authenticationUseCases.login(credentials = UserCredentials(username = "su", password = "su")).successData().account)
    }

    /** Verifies that seeding a stored super user restores its permissions, keeping its password. */
    @Test
    fun seedSuperUserRestoresPermissions() = runServerTest {
        val useCases = dependency.get().useCases
        val uuid = uuid()
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedSuperUser(uuid = uuid, username = "su", password = "su"))
        val account = useCases.accountUseCases.getAccount(userUuid = uuid).successData()
        assertIs<Outcome.Success<Unit>>(value = useCases.accountUseCases.upsertAccount(account = account.copy(permissions = emptyMap())))

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedSuperUser(uuid = uuid, username = "su", password = "other"))

        assertEquals(expected = account.permissions, actual = useCases.accountUseCases.getAccount(userUuid = uuid).successData().permissions)
        assertIs<Outcome.Success<*>>(value = useCases.authenticationUseCases.login(credentials = UserCredentials(username = "su", password = "su")))
    }

    /** Verifies that seeding a super user whose username belongs to the account of another user reports [Outcome.Error] and stores no account. */
    @Test
    fun seedSuperUserWithTakenUsername() = runServerTest {
        val useCases = dependency.get().useCases
        val uuid = uuid()

        assertEquals(expected = Outcome.Error, actual = useCases.seedUseCases.seedSuperUser(uuid = uuid, username = FakeData.account.username, password = "su"))

        assertEquals(expected = Outcome.NotFound, actual = useCases.accountUseCases.getAccount(userUuid = uuid))
    }

    /** Verifies that seeded translations are retrievable sorted by language and key. */
    @Test
    fun seedAndGetTranslations() = runServerTest {
        val useCases = dependency.get().useCases

        assertTrue(actual = useCases.translationUseCases.getTranslations().successData().isEmpty())
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedTranslations(translations = FakeData.translations))
        assertEquals(
            expected = FakeData.translations.sortedWith(comparator = compareBy({ it.languageIso }, { it.key })),
            actual = useCases.translationUseCases.getTranslations().successData()
        )
    }

    /** Verifies that seeding inserts only the translations that are not stored yet, keeping the stored ones unchanged. */
    @Test
    fun seedKeepsStoredTranslations() = runServerTest {
        val useCases = dependency.get().useCases
        val stored = FakeData.translations.first().copy(modifiedAt = now(), value = "edited")

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedTranslations(translations = listOf(element = stored)))
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedTranslations(translations = FakeData.translations))

        val translations = useCases.translationUseCases.getTranslations().successData()
        assertEquals(expected = FakeData.translations.size, actual = translations.size)
        assertEquals(expected = stored, actual = translations.single { it.languageIso == stored.languageIso && it.key == stored.key })
    }

    /** Verifies that seeding a list holding the same language and key twice stores the first one. */
    @Test
    fun seedIgnoresDuplicateTranslations() = runServerTest {
        val useCases = dependency.get().useCases
        val translation = FakeData.translations.first()

        assertEquals(
            expected = Outcome.Success(data = Unit),
            actual = useCases.seedUseCases.seedTranslations(translations = listOf(translation, translation.copy(value = "duplicate")))
        )
        assertEquals(expected = listOf(element = translation), actual = useCases.translationUseCases.getTranslations().successData())
    }

    /** Verifies that seeding stores every entity of the seed, and that a seeded account can log in. */
    @Test
    fun seedEntities() = runServerTest {
        val useCases = dependency.get().useCases

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedEntities(seed = seed))

        assertEquals(expected = seedUser, actual = useCases.userUseCases.getUser(uuid = seedUser.uuid).successData())
        assertEquals(expected = seedRegistration.user, actual = useCases.userUseCases.getUser(uuid = seedRegistration.user.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = useCases.accountUseCases.getAccount(userUuid = seedUser.uuid))
        assertEquals(expected = seedRegistration.account, actual = useCases.accountUseCases.getAccount(userUuid = seedRegistration.user.uuid).successData())
        assertEquals(expected = seedRegistration.account, actual = useCases.authenticationUseCases.login(credentials = seedRegistration.credentials).successData().account)
        assertEquals(expected = FakeData.task, actual = useCases.taskUseCases.getTask(uuid = FakeData.task.uuid).successData())
        assertEquals(expected = FakeData.equipment, actual = useCases.equipmentUseCases.getEquipment(uuid = FakeData.equipment.uuid).successData())
        assertEquals(expected = FakeData.building, actual = useCases.buildingUseCases.getBuilding(uuid = FakeData.building.uuid).successData())
        assertEquals(expected = FakeData.material, actual = useCases.materialUseCases.getMaterial(uuid = FakeData.material.uuid).successData())
        assertEquals(expected = FakeData.buildingEquipment, actual = useCases.buildingEquipmentUseCases.getBuildingEquipment(uuid = FakeData.buildingEquipment.uuid).successData())
        assertEquals(expected = FakeData.buildingMaterial, actual = useCases.buildingMaterialUseCases.getBuildingMaterial(uuid = FakeData.buildingMaterial.uuid).successData())
        assertEquals(expected = FakeData.buildingUser, actual = useCases.buildingUserUseCases.getBuildingUser(uuid = FakeData.buildingUser.uuid).successData())
        assertEquals(expected = FakeData.equipmentUser, actual = useCases.equipmentUserUseCases.getEquipmentUser(uuid = FakeData.equipmentUser.uuid).successData())
        assertEquals(expected = listOf(element = FakeData.registry), actual = registries())
    }

    /** Verifies that seeding inserts only the entities that are not stored yet, keeping the stored ones unchanged. */
    @Test
    fun seedKeepsStoredEntities() = runServerTest {
        val useCases = dependency.get().useCases
        val stored = FakeData.building.copy(name = "edited")
        assertIs<Outcome.Success<Unit>>(value = useCases.buildingUseCases.upsertBuilding(building = stored))

        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedEntities(seed = seed))
        assertEquals(expected = Outcome.Success(data = Unit), actual = useCases.seedUseCases.seedEntities(seed = seed))

        assertEquals(expected = stored, actual = useCases.buildingUseCases.getBuilding(uuid = stored.uuid).successData())
        assertEquals(expected = FakeData.task, actual = useCases.taskUseCases.getTask(uuid = FakeData.task.uuid).successData())
        assertEquals(expected = listOf(element = FakeData.registry), actual = registries())
    }

    /** Verifies that seeding a list holding the same uuid twice stores the first one. */
    @Test
    fun seedIgnoresDuplicateEntities() = runServerTest {
        val useCases = dependency.get().useCases

        assertEquals(
            expected = Outcome.Success(data = Unit),
            actual = useCases.seedUseCases.seedEntities(seed = Seed(tasks = listOf(FakeData.task, FakeData.task.copy(title = "duplicate"))))
        )
        assertEquals(expected = FakeData.task, actual = useCases.taskUseCases.getTask(uuid = FakeData.task.uuid).successData())
    }

    /** Verifies that seeding skips the account of a user that already has one and the account whose username is taken, storing their users. */
    @Test
    fun seedSkipsStoredAccounts() = runServerTest {
        val useCases = dependency.get().useCases
        val sameUser = FakeData.registrationForm.copy(account = FakeData.account.copy(username = "renamed"))
        val sameUsername = registrationForm(username = FakeData.account.username, permissions = emptyMap())

        assertEquals(
            expected = Outcome.Success(data = Unit),
            actual = useCases.seedUseCases.seedEntities(seed = Seed(registrations = listOf(sameUser, sameUsername)))
        )

        assertEquals(expected = FakeData.account, actual = useCases.accountUseCases.getAccount(userUuid = FakeData.user.uuid).successData())
        assertEquals(expected = sameUsername.user, actual = useCases.userUseCases.getUser(uuid = sameUsername.user.uuid).successData())
        assertEquals(expected = Outcome.NotFound, actual = useCases.accountUseCases.getAccount(userUuid = sameUsername.user.uuid))
    }

    /** Verifies that when the database fails, its writes report [Outcome.Error]. */
    @Test
    fun databaseFailureIsAnError() = runServerTest {
        val seedUseCases = dependency.get().useCases.seedUseCases
        dependency.get().database.reset()

        listOf(
            "seed" to seedUseCases.seed(),
            "seedSuperUser" to seedUseCases.seedSuperUser(uuid = uuid(), username = "su", password = "su"),
            "seedTranslations" to seedUseCases.seedTranslations(translations = FakeData.translations),
            "seedEntities" to seedUseCases.seedEntities(seed = seed)
        ).forEach { (name, outcome) -> assertEquals(expected = Outcome.Error, actual = outcome, message = name) }
    }
}
