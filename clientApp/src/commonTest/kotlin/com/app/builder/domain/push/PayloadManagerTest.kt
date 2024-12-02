package com.app.builder.domain.push

import kotlin.test.Test
import kotlin.test.assertNotNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import com.app.builder.domain.EntityType
import com.app.builder.domain.PushPayload
import com.app.builder.domain.scheduler.Job
import com.app.builder.test.TestCase

class PayloadManagerTest: TestCase() {

    private suspend fun handlePushPayload(payload: PushPayload) =
        (dependency.get().payloadService as PayloadManager).handlePushPayload(payload = payload)

    /** Verifies that handling a [PushPayload.Flags] payload queues a GET job for client feature flags. */
    @Test
    fun handleFlagsPayload() = runUnitTest {
        val jobDao = dependency.get().database.jobQueries

        handlePushPayload(payload = PushPayload.Flags())
        val jobs = jobDao.getJobsByState(states = Job.State.entries).awaitAsList()

        assertNotNull(actual = jobs.find { it.entityType == EntityType.CLIENT_FLAG && it.type == Job.Type.GET })
    }

    /** Verifies that handling a [PushPayload.Configs] payload queues a GET job for client remote configs. */
    @Test
    fun handleConfigsPayload() = runUnitTest {
        val jobDao = dependency.get().database.jobQueries

        handlePushPayload(payload = PushPayload.Configs())
        val jobs = jobDao.getJobsByState(states = Job.State.entries).awaitAsList()

        assertNotNull(actual = jobs.find { it.entityType == EntityType.CLIENT_CONFIG && it.type == Job.Type.GET })
    }
}
