package com.app.builder.ui.component.link

import kotlin.uuid.Uuid
import com.app.builder.ui.component.field.LinkExtra
import com.app.builder.ui.component.field.LinkItem

/**
 * An entity linked to the one being viewed or edited.
 *
 * @property uuid The unique identifier of the linked entity.
 * @property name The name of the linked entity.
 * @property quantity The quantity of the link as typed, or `null` when the link has no quantity.
 */
data class Link(
    val uuid: Uuid,
    val name: String = "",
    val quantity: String? = null,
) {
    /** Whether [quantity] is set and not a whole number of at least one. */
    val quantityError: Boolean
        get() = quantity != null && quantity.toIntOrNull()?.let { it >= 1 } != true
}

/**
 * The links to write so the stored links match the edited ones.
 *
 * @property upserted The links that are new or have another quantity.
 * @property removed The stored links that are no longer linked.
 */
data class LinkChanges(
    val upserted: List<Link> = emptyList(),
    val removed: List<Link> = emptyList(),
)

/**
 * Compares the [edited] links with the [stored] ones.
 *
 * @param edited The links as edited.
 * @param stored The links as stored.
 * @return The edited links missing from [stored] or with another quantity there, and the stored links missing from [edited].
 */
fun linkChanges(edited: List<Link>, stored: List<Link>): LinkChanges {
    val storedByUuid = stored.associateBy { it.uuid }
    val editedUuids = edited.map { it.uuid }.toSet()
    return LinkChanges(
        upserted = edited.filter { link ->
            val storedLink = storedByUuid[link.uuid]
            storedLink == null || storedLink.quantity?.toIntOrNull() != link.quantity?.toIntOrNull()
        },
        removed = stored.filter { it.uuid !in editedUuids }
    )
}

/**
 * Converts this link into a [LinkItem], with its quantity as a number extra.
 *
 * @return The corresponding [LinkItem].
 */
fun Link.toLinkItem(): LinkItem = LinkItem(
    uuid = uuid.toString(),
    name = name,
    extra = quantity?.let { LinkExtra.Number(value = it, error = quantityError) }
)
