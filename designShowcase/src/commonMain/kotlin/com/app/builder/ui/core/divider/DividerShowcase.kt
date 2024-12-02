package com.app.builder.ui.core.divider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ShowcaseSection
import com.app.builder.ui.core.text.Text

/** Displays horizontal and vertical dividers separating sample content. */
@Composable
fun DividerShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Horizontal") { HorizontalDividerPreview() }
        ShowcaseSection(title = "Vertical") { VerticalDividerPreview() }
    }
}

@Composable
private fun HorizontalDividerPreview() {
    Column {
        Text(text = "Top")
        Divider(modifier = Modifier.padding(vertical = 8.dp), axis = Axis.HORIZONTAL)
        Text(text = "Bottom")
    }
}

@Composable
private fun VerticalDividerPreview() {
    Row(modifier = Modifier.height(height = 32.dp)) {
        Text(text = "Start")
        Divider(modifier = Modifier.padding(horizontal = 8.dp), axis = Axis.VERTICAL)
        Text(text = "End")
    }
}
