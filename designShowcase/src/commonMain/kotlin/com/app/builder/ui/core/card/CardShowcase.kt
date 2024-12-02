package com.app.builder.ui.core.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ShowcaseSection
import com.app.builder.ui.core.text.Text

/** Displays the selectable card component. */
@Composable
fun CardShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Selectable Card") { CardPreview() }
    }
}

@Composable
private fun CardPreview() {
    var selected by remember { mutableStateOf(value = false) }
    Card(
        modifier = Modifier.clickable { selected = !selected },
        selected = selected,
        content = {
            Text(
                modifier = Modifier.padding(all = 16.dp),
                text = "Tap. Selected: $selected"
            )
        }
    )
}
