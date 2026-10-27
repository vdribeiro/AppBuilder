package com.app.builder.core.nfc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import com.app.builder.domain.EntityType
import com.app.builder.test.TestCase

class TagProviderTest: TestCase() {

    /** Verifies that a tag sent to the provider is emitted to collectors of [TagProvider.tags]. */
    @Test
    fun sentTagIsEmitted() = runUnitTest {
        val tag = NfcTag(id = "tag", records = listOf(NfcRecord.Open(entityType = EntityType.TASK)))

        TagProvider.sendTag(tag = tag)

        assertEquals(expected = tag, actual = TagProvider.tags.first())
    }

    /** Verifies that [TagProvider.reset] discards buffered tags, so the next collected tag is one sent afterward. */
    @Test
    fun resetDiscardsBufferedTags() = runUnitTest {
        TagProvider.sendTag(tag = NfcTag(id = "first", records = emptyList()))
        TagProvider.sendTag(tag = NfcTag(id = "second", records = emptyList()))

        TagProvider.reset()
        val next = NfcTag(id = "next", records = emptyList())
        TagProvider.sendTag(tag = next)

        assertEquals(expected = next, actual = TagProvider.tags.first())
    }
}
