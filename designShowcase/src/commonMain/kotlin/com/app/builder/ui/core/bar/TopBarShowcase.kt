package com.app.builder.ui.core.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.builder.ShowcaseSection
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.image.Icon

/** Displays the small, medium, and large top bar variants. */
@Composable
fun TopBarShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Top bar") { TopBarPreview() }
        ShowcaseSection(title = "Medium Top bar") { TopBarMediumPreview() }
        ShowcaseSection(title = "Large Top bar") { TopBarLargePreview() }
    }
}

@Composable
private fun TopBarPreview() {
    TopBar(
        variant = TopBarVariant.SMALL,
        title = { androidx.compose.material3.Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}

@Composable
private fun TopBarMediumPreview() {
    TopBar(
        variant = TopBarVariant.MEDIUM,
        title = { androidx.compose.material3.Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}

@Composable
private fun TopBarLargePreview() {
    TopBar(
        variant = TopBarVariant.LARGE,
        title = { androidx.compose.material3.Text(text = "Title") },
        navigationIcon = { Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack) }) },
        actions = {
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Apps) })
            Button(enabled = true, onClick = {}, content = { Icon(imageVector = Icons.Default.Search) })
        }
    )
}
