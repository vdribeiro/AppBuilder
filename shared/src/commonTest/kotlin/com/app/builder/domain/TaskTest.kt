package com.app.builder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.app.builder.core.locale.now
import com.app.builder.data.serializer.decode
import com.app.builder.data.serializer.encode
import com.app.builder.test.FakeData
import com.app.builder.test.TestCase

class TaskTest: TestCase() {

    /** Verifies a default task is an empty, undeleted TODO stamped with the current time and a fresh uuid. */
    @Test
    fun defaults() = runUnitTest {
        val task = Task()
        assertEquals(expected = now(), actual = task.modifiedAt)
        assertNull(actual = task.deletedAt)
        assertEquals(expected = "", actual = task.title)
        assertEquals(expected = "", actual = task.description)
        assertEquals(expected = Task.State.TODO, actual = task.state)
        assertNotEquals(illegal = task.uuid, actual = Task().uuid)
    }

    /** Verifies a task survives a JSON round trip. */
    @Test
    fun serializationRoundTrip() = runUnitTest {
        val task = FakeData.task.copy(state = Task.State.COMPLETE, deletedAt = now())
        assertEquals(
            expected = task,
            actual = decode(value = assertNotNull(actual = encode(value = task)))
        )
    }

    /** Verifies a task decodes from JSON holding only its uuid, filling every other field from its defaults. */
    @Test
    fun decodeFillsDefaults() = runUnitTest {
        val uuid = FakeData.task.uuid
        val task = assertNotNull(actual = decode<Task>(value = """{"uuid":"$uuid"}"""))
        assertEquals(expected = uuid, actual = task.uuid)
        assertEquals(expected = now(), actual = task.modifiedAt)
        assertEquals(expected = Task.State.TODO, actual = task.state)
    }
}
