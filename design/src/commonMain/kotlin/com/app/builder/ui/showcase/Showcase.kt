package com.app.builder.ui.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.core.divider.Divider
import com.app.builder.ui.core.text.Text

/**
 * A category with a title, content and divider.
 *
 * @param title The category's title.
 * @param content The category's content.
 */
@Composable
fun CategorySection(title: String, content: @Composable () -> Unit = {}) {
    val typography = LocalTypography.current
    Column {
        Text(text = title, translate = false, style = typography.headlineSmall)
        content()
        Divider(modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))
    }
}

/**
 * A section with a title and content.
 *
 * @param title The section's title.
 * @param content The section's content.
 */
@Composable
fun ShowcaseSection(title: String, content: @Composable () -> Unit) {
    val typography = LocalTypography.current
    Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        Text(text = title, translate = false, style = typography.titleMedium)
        content()
    }
}
