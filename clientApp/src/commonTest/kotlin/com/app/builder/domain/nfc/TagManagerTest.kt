package com.app.builder.domain.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import com.app.builder.Dependency.getUserDependency
import com.app.builder.core.nfc.NfcRecord
import com.app.builder.core.nfc.NfcTag
import com.app.builder.domain.EntityType
import com.app.builder.domain.deeplink.DeepLink
import com.app.builder.domain.gateway.UseCases
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TagManagerTest: TestCase() {

    /** The tag reads fed to the manager under test. */
    private val tags = Channel<NfcTag>(capacity = Channel.BUFFERED)

    /**
     * Builds a [TagManager] that processes [tags].
     *
     * @return The fixture [TagManager] instance.
     */
    private suspend fun tagManager(): TagManager = TagManager(appGraph = dependency.get(), tags = tags.receiveAsFlow())

    /**
     * Logs in as the admin, so tag writes go through a user allowed to make them.
     *
     * @return The admin's [UseCases].
     */
    private suspend fun loggedInUseCases(): UseCases {
        val useCases = dependency.get()
            .getUserDependency(user = FakeData.adminUser)
            .useCases
        useCases.authenticationUseCases.login(credentials = FakeData.adminCredentials)
        return useCases
    }

    /**
     * Sends a tag carrying a single [record].
     *
     * @param record The record the tag carries.
     */
    private suspend fun sendTag(record: NfcRecord) = tags.send(element = NfcTag(id = "tag", records = listOf(record)))

    /** Verifies that an open record is forwarded as a deep link to its entity. */
    @Test
    fun openRecordSendsADeepLink() = runUnitTest {
        val uuid = Uuid.random()
        val tagManager = tagManager()

        tagManager.start()
        sendTag(record = NfcRecord.Open(entityType = EntityType.TASK, entityUuid = uuid))
        val deepLink = DeepLink.deepLinks.first()
        tagManager.stop()

        assertEquals(expected = DeepLink.Entity(entityUuid = uuid, entityType = EntityType.TASK), actual = deepLink)
    }

    /** Verifies that a task upsert record creates a task with the record's uuid. */
    @Test
    fun taskUpsertRecordCreatesTheTask() = runUnitTest {
        val useCases = loggedInUseCases()
        val uuid = Uuid.random()
        val tagManager = tagManager()

        tagManager.start()
        sendTag(record = NfcRecord.Upsert(entityType = EntityType.TASK, entityUuid = uuid))
        useCases.taskUseCases.observeTask(uuid = uuid).first { it != null }
        tagManager.stop()
    }

    /** Verifies that a task upsert record without a uuid creates a task with a new one. */
    @Test
    fun taskUpsertRecordWithoutUuidCreatesANewTask() = runUnitTest {
        val useCases = loggedInUseCases()
        val tagManager = tagManager()

        tagManager.start()
        sendTag(record = NfcRecord.Upsert(entityType = EntityType.TASK))
        val created = useCases.taskUseCases.observeTasks().first { it.isNotEmpty() }
        tagManager.stop()

        assertEquals(expected = 1, actual = created.size)
    }

    /** Verifies that a user upsert record creates a user with the record's uuid. */
    @Test
    fun userUpsertRecordCreatesTheUser() = runUnitTest {
        val useCases = loggedInUseCases()
        val uuid = Uuid.random()
        val tagManager = tagManager()

        tagManager.start()
        sendTag(record = NfcRecord.Upsert(entityType = EntityType.USER, entityUuid = uuid))
        useCases.userUseCases.observeUser(uuid = uuid).first { it != null }
        tagManager.stop()
    }

    /** Verifies that an upsert record for an entity type tags cannot write is ignored, while later tags are still processed. */
    @Test
    fun unhandledUpsertRecordIsIgnored() = runUnitTest {
        val useCases = loggedInUseCases()
        val uuid = Uuid.random()
        val tagManager = tagManager()

        tagManager.start()
        sendTag(record = NfcRecord.Upsert(entityType = EntityType.REGISTRY, entityUuid = Uuid.random()))
        sendTag(record = NfcRecord.Upsert(entityType = EntityType.TASK, entityUuid = uuid))
        val tasks = useCases.taskUseCases.observeTasks().first { it.isNotEmpty() }
        tagManager.stop()

        assertEquals(expected = listOf(uuid), actual = tasks.map { it.uuid })
    }

    /** Verifies that starting twice keeps a single collector, so each tag is processed once. */
    @Test
    fun startingTwiceProcessesEachTagOnce() = runUnitTest {
        val tagManager = tagManager()

        tagManager.start()
        tagManager.start()
        // Deep links are conflated, so each one is read before the next tag is sent.
        sendTag(record = NfcRecord.Open(entityType = EntityType.TASK))
        val first = DeepLink.deepLinks.first()
        sendTag(record = NfcRecord.Open(entityType = EntityType.USER))
        val second = DeepLink.deepLinks.first()
        tagManager.stop()

        assertEquals(expected = listOf(EntityType.TASK, EntityType.USER), actual = listOf(first, second).map { (it as DeepLink.Entity).entityType })
    }

    /** Verifies that a stopped manager leaves tags unprocessed, and processes them again once restarted. */
    @Test
    fun stopPausesProcessingUntilRestarted() = runUnitTest {
        val tagManager = tagManager()
        tagManager.start()
        tagManager.stop()

        sendTag(record = NfcRecord.Open(entityType = EntityType.TASK))
        testScheduler.runCurrent()
        assertTrue(actual = tags.tryReceive().isSuccess, message = "the tag was consumed while stopped")

        tagManager.start()
        sendTag(record = NfcRecord.Open(entityType = EntityType.USER))
        val deepLink = DeepLink.deepLinks.first()
        tagManager.stop()

        assertEquals(expected = DeepLink.Entity(entityUuid = null, entityType = EntityType.USER), actual = deepLink)
    }

    /** Verifies that tags read while signed out are written as the guest, who is not allowed to write, so nothing is created. */
    @Test
    fun guestUpsertRecordWritesNothing() = runUnitTest {
        val guestUseCases = dependency.get().userGraph.useCases
        val adminUseCases = dependency.get().getUserDependency(user = FakeData.adminUser).useCases
        val tagManager = tagManager()

        tagManager.start()
        sendTag(record = NfcRecord.Upsert(entityType = EntityType.TASK, entityUuid = Uuid.random()))
        sendTag(record = NfcRecord.Open(entityType = EntityType.TASK))
        DeepLink.deepLinks.first()
        tagManager.stop()

        assertEquals(expected = null, actual = guestUseCases.taskUseCases.observeTasks().firstOrNull())
        assertTrue(actual = adminUseCases.taskUseCases.observeTasks().first().isEmpty())
    }
}
