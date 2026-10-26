package com.app.builder.domain.gateway.task

import kotlin.test.Test
import kotlin.test.assertEquals
import com.app.builder.core.locale.now
import com.app.builder.domain.Task
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class MappersTest: TestCase() {

    /** Verifies that a task survives a round trip through its database schema, deletion timestamp and state included. */
    @Test
    fun taskRoundTrip() = runUnitTest {
        assertEquals(expected = FakeData.task, actual = FakeData.task.toTaskSchema().toTask())

        val deleted = FakeData.task.copy(deletedAt = now(), state = Task.State.entries.last())
        assertEquals(expected = deleted, actual = deleted.toTaskSchema().toTask())
    }
}
