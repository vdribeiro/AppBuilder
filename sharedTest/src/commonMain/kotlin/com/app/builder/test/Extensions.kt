package com.app.builder.test

import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog

/** Print the complete hierarchy. */
fun ComposeUiTest.printEverything(tag: String = "Full Tree") {
    onRoot(useUnmergedTree = true).printToLog(tag = tag)
}

/** Verify the number of items in a collection. */
fun SemanticsNodeInteraction.count(count: Int): SemanticsNodeInteraction =
    if (runCatching { onChildren().assertCountEquals(expectedSize = count) }.isSuccess) this else
        assert(matcher = SemanticsMatcher(description = "Has $count items") { node ->
            val collectionInfo = node.findCollectionInfo()
            collectionInfo != null && (collectionInfo.rowCount == count || collectionInfo.columnCount == count)
        })

/**
 * Depth first search for the closest [SemanticsProperties.CollectionInfo] of this node or any of its descendants.
 *
 * @return The [CollectionInfo] of the closest collection, or `null` if the subtree holds none.
 */
private fun SemanticsNode.findCollectionInfo(): CollectionInfo? =
    config.getOrNull(key = SemanticsProperties.CollectionInfo) ?: children.firstNotNullOfOrNull { it.findCollectionInfo() }