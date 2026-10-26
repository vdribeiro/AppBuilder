package com.app.builder.domain.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid
import com.app.builder.core.locale.now
import com.app.builder.data.http.Header
import com.app.builder.domain.EntityType
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that a job survives a round trip through its database schema, retry state included. */
    @Test
    fun jobRoundTrip() = runUnitTest {
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.GET)
        assertEquals(expected = job, actual = job.toJobSchema().toJob())

        val retried = Job(
            userUuid = FakeData.adminUser.uuid,
            entityUuid = FakeData.task.uuid,
            entityType = EntityType.TASK,
            type = Job.Type.POST,
            conflictPolicy = Job.ConflictPolicy.REPLACE,
            payload = "payload",
            state = Job.State.FAILED,
            attempt = 3,
            nextAttemptAt = now()
        )
        assertEquals(expected = retried, actual = retried.toJobSchema().toJob())
    }

    /** Verifies that a job identifies itself to the server through its request uuid and creation instant. */
    @Test
    fun jobHeaderMap() = runUnitTest {
        val job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.GET)

        assertEquals(
            expected = mapOf(Header.RequestUuid to job.uuid.toString(), Header.RequestUtc to job.utc.toString()),
            actual = job.toHeaderMap()
        )
    }
}
