package com.app.builder.domain.scheduler

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.Uuid
import com.app.builder.data.http.plugin.InternetDisabledException
import com.app.builder.data.http.plugin.NetworkDisabledException
import com.app.builder.domain.EntityType
import com.app.builder.domain.scheduler.JobResult.NoOp.toJobResult
import com.app.builder.test.TestCase

class JobResultTest: TestCase() {

    /** Verifies internet-disabled failures map to a retryable job result. */
    @Test
    fun internetFailuresAreRetryable() = runUnitTest {
        assertEquals(expected = JobResult.Retry, actual = InternetDisabledException().toJobResult())
    }

    /** Verifies other failures map to a terminal error job result. */
    @Test
    fun otherFailuresAreTerminal() = runUnitTest {
        val throwable = NetworkDisabledException()

        val result = assertIs<JobResult.Error>(value = throwable.toJobResult())
        assertEquals(expected = throwable, actual = result.error)
        assertEquals(expected = JobResult.Error(error = throwable), actual = result)
    }

    /** Verifies [NoOpJobFactory] resolves any job to a no-op result. */
    @Test
    fun noOpFactoryResolvesToNoOp() = runUnitTest {
        val factory = NoOpJobFactory()

        assertEquals(expected = JobResult.NoOp, actual = factory.resolve(job = Job(userUuid = Uuid.NIL, entityType = EntityType.TASK, type = Job.Type.GET)).invoke())
    }
}
