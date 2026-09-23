package com.app.builder.ui.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.text.Text

/** Displays every text style of the current typography scale, from display down to label. */
@Composable
fun TypographyShowcase() {
    val typography = LocalTypography.current
    LazyColumn(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        item { Text(text = "Display large", style = typography.displayLarge) }
        item { Text(text = "Display medium", style = typography.displayMedium) }
        item { Text(text = "Display small", style = typography.displaySmall) }
        item { Text(text = "Headline large", style = typography.headlineLarge) }
        item { Text(text = "Headline medium", style = typography.headlineMedium) }
        item { Text(text = "Headline small", style = typography.headlineSmall) }
        item { Text(text = "Title large", style = typography.titleLarge) }
        item { Text(text = "Title medium", style = typography.titleMedium) }
        item { Text(text = "Title small", style = typography.titleSmall) }
        item { Text(text = "Body large", style = typography.bodyLarge) }
        item { Text(text = "Body medium", style = typography.bodyMedium) }
        item { Text(text = "Body small", style = typography.bodySmall) }
        item { Text(text = "Label large", style = typography.labelLarge) }
        item { Text(text = "Label medium", style = typography.labelMedium) }
        item { Text(text = "Label small", style = typography.labelSmall) }
    }
}
