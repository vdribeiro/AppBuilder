package com.app.builder.ui.core.image

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import appbuilder.designshowcase.generated.resources.Res
import appbuilder.designshowcase.generated.resources.sample_image
import com.app.builder.ShowcaseSection
import com.app.builder.ui.LocalColorScheme

/** Displays icons and images, including content scaling options. */
@Composable
fun ImageShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Icon") { IconPreview() }
        ShowcaseSection(title = "Image") { ImagePreview() }
    }
}

@Composable
private fun IconPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        val colorScheme = LocalColorScheme.current
        Icon(imageVector = Icons.Default.Apps, tint = colorScheme.primary)
        Icon(imageVector = Icons.Default.Apps)
        Icon()
    }
}

@Composable
private fun ImagePreview() {
    Image(
        modifier = Modifier.size(size = 120.dp),
        image = Image(path = "drawable/sample_image.xml", drawable = Res.drawable.sample_image),
        contentDescription = "Sample image",
    )
    Image(
        modifier = Modifier.size(size = 120.dp),
        image = Image(path = "drawable/sample_image.xml", drawable = Res.drawable.sample_image),
        contentDescription = "Sample image",
        contentScale = ContentScale.None
    )
}
