package com.app.builder.ui.core.image

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import appbuilder.design.generated.resources.Res
import coil3.compose.AsyncImage
import com.app.builder.core.platform.OS
import com.app.builder.core.platform.platform
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.ui.Preview
import com.app.builder.ui.getTranslation

/**
 * Resource with optional remote address, physical path and generated [DrawableResource] reference.
 *
 * @property url Remote address of the image.
 * @property path The relative file path locating the asset directory.
 * @property drawable The generated [DrawableResource] reference.
 */
@Stable
data class Image(
    val url: String? = null,
    val path: String? = null,
    val drawable: DrawableResource? = null
)

/**
 * A platform-aware image loader that switches drawing implementations based on runtime environment.
 * It first tries to load and draw a remote image from [Image.url] using [AsyncImage].
 * Otherwise, for local images, when executing on mobile platforms outside of layout preview inspection modes,
 * it routes through the URI-based overload to dynamically resolve resources using URI paths and streaming.
 * Otherwise, it routes through [painterResource] to statically decode local bundled [DrawableResource] blocks.
 *
 * @param modifier The [Modifier] to apply to the resulting image container layout boundaries.
 * @param image The image resource.
 * @param contentDescription Localized accessibility description text. Can be null.
 * @param contentScale Algorithm strategy used to determine target dimension scaling adjustments. Defaults to [ContentScale.Crop].
 */
@Composable
fun Image(
    modifier: Modifier = Modifier,
    image: Image,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    when {
        image.url != null -> AsyncImage(
            modifier = modifier,
            model = image.url,
            contentDescription = contentDescription?.let { getTranslation(key = it) },
            contentScale = contentScale,
            onError = { Telemetry.error(tag = TAG, message = "Unable to draw image", throwable = it.result.throwable) },
        )

        image.path != null && (platform.os == OS.Android || platform.os == OS.Ios) -> AsyncImage(
            modifier = modifier,
            model = Res.getUri(path = image.path),
            contentDescription = contentDescription?.let { getTranslation(key = it) },
            contentScale = contentScale,
            onError = { Telemetry.error(tag = TAG, message = "Unable to draw image", throwable = it.result.throwable) },
        )

        image.drawable != null && (LocalInspectionMode.current || platform.os == OS.Windows || platform.os == OS.Mac || platform.os == OS.Linux || platform.os == OS.Web) -> Image(
            modifier = modifier,
            painter = painterResource(resource = image.drawable),
            contentDescription = contentDescription?.let { getTranslation(key = it) },
            contentScale = contentScale
        )
    }
}

@Preview
@Composable
private fun ImagePreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Image(image = Image(url = "https://kotlinlang.org/images/compose-multiplatform/hero/compose-multiplatform-logo.svg"))
    }
}

private const val TAG = "Image"
