package com.app.builder.ui.component.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.app.builder.core.security.uuid
import com.app.builder.test.TestCase
import com.app.builder.ui.component.field.LinkExtra
import com.app.builder.ui.component.field.LinkItem

class LinkTest: TestCase() {

    /** Verifies that a quantity is an error unless it is missing or a whole number of at least one. */
    @Test
    fun quantityError() = runUnitTest {
        assertFalse(actual = Link(uuid = uuid()).quantityError)
        assertFalse(actual = Link(uuid = uuid(), quantity = "1").quantityError)
        listOf("", "0", "-2", "1.5", "many").forEach { quantity ->
            assertTrue(actual = Link(uuid = uuid(), quantity = quantity).quantityError)
        }
    }

    /** Verifies that the changes hold the new links, the links with another quantity and the links no longer linked, and leave out the unchanged ones. */
    @Test
    fun changes() = runUnitTest {
        val kept = Link(uuid = uuid(), name = "Kept", quantity = "3")
        val changed = Link(uuid = uuid(), name = "Changed", quantity = "3")
        val removed = Link(uuid = uuid(), name = "Removed", quantity = "3")
        val added = Link(uuid = uuid(), name = "Added", quantity = "1")

        val changes = linkChanges(
            edited = listOf(kept.copy(quantity = "03"), changed.copy(quantity = "8"), added),
            stored = listOf(kept, changed, removed)
        )

        assertEquals(expected = listOf(changed.copy(quantity = "8"), added), actual = changes.upserted)
        assertEquals(expected = listOf(element = removed), actual = changes.removed)
    }

    /** Verifies that links without a quantity only change when they are added or removed. */
    @Test
    fun changesWithoutQuantity() = runUnitTest {
        val kept = Link(uuid = uuid(), name = "Kept")
        val removed = Link(uuid = uuid(), name = "Removed")
        val added = Link(uuid = uuid(), name = "Added")

        val changes = linkChanges(edited = listOf(kept, added), stored = listOf(kept, removed))

        assertEquals(expected = LinkChanges(upserted = listOf(element = added), removed = listOf(element = removed)), actual = changes)
    }

    /** Verifies that a link becomes a link item carrying its quantity as a number extra, and none without a quantity. */
    @Test
    fun toLinkItem() = runUnitTest {
        val link = Link(uuid = uuid(), name = "Cement")

        assertEquals(expected = LinkItem(uuid = link.uuid.toString(), name = "Cement"), actual = link.toLinkItem())
        assertEquals(
            expected = LinkItem(uuid = link.uuid.toString(), name = "Cement", extra = LinkExtra.Number(value = "0", error = true)),
            actual = link.copy(quantity = "0").toLinkItem()
        )
    }
}
