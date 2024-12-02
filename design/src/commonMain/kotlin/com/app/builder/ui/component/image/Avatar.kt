package com.app.builder.ui.component.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.LocalTypography
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Image
import com.app.builder.ui.core.text.Text

/**
 * An avatar showing the image when provided, or the capitalized initials of [name] otherwise.
 * The background color is derived deterministically from [name], so the same name always renders with the same color.
 *
 * @param modifier The [Modifier] to be applied to the root layout.
 * @param name The name to fit in the avatar. Only the first letter of the first and last name will show capitalized if no image is provided.
 * @param image The image resource.
 * @param contentDescription Localized accessibility description text. Can be null.
 * @param size The diameter of the avatar. Defaults to 40.dp.
 * @param shape The [Shape] the avatar is clipped to. Defaults to [CircleShape].
 */
@Composable
fun Avatar(
    modifier: Modifier = Modifier,
    name: String? = null,
    image: Image? = null,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    shape: Shape = CircleShape
) {
    val colorScheme = LocalColorScheme.current
    val typography = LocalTypography.current
    val density = LocalDensity.current

    val (containerColor, onContainerColor) = avatarColors(name = name, colorScheme = colorScheme)
    val scaledFontSize = with(receiver = density) { (size * 0.5f).toSp() }

    Box(
        modifier = modifier
            .size(size = size)
            .clip(shape = shape)
            .background(color = containerColor),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) Image(
            modifier = Modifier.size(size = size),
            image = image,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
        ) else Text(
            text = initials(name = name),
            translate = false,
            style = typography.titleMedium.copy(fontSize = scaledFontSize),
            color = onContainerColor,
        )
    }
}

/**
 * Picks a container/on-container color pair from [colorScheme], deterministically chosen from [name] so the same name always maps to the same pair.
 *
 * @param name The name to derive the color pair from. Falls back to the secondary pair if null or blank.
 * @param colorScheme The color scheme to pick the pair from.
 * @return A container color paired with its contrasting on-container color.
 */
private fun avatarColors(name: String?, colorScheme: ColorScheme): Pair<Color, Color> {
    val palette = listOf(
        colorScheme.primaryContainer to colorScheme.onPrimaryContainer,
        colorScheme.secondaryContainer to colorScheme.onSecondaryContainer,
        colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer,
        colorScheme.errorContainer to colorScheme.onErrorContainer,
    )
    if (name.isNullOrBlank()) return palette[1]
    val index = name.trim().lowercase().hashCode().mod(other = palette.size)
    return palette[index]
}

/**
 * Extracts the capitalized initials of a name, keeping only the first letter of the first and last words.
 *
 * @param name The name to extract initials from. Can be null or blank.
 * @return The capitalized initials, or an empty string if [name] is null or blank.
 */
private fun initials(name: String?): String {
    val parts = name?.trim()?.split(Regex(pattern = "\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first().take(n = 1).uppercase()
        else -> (parts.first().take(n = 1) + parts.last().take(n = 1)).uppercase()
    }
}

@Preview
@Composable
private fun AvatarPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Avatar()
        Avatar(name = "Bito")
        Avatar(name = "Sofia and Vitor")
        Avatar(name = "V R", size = 200.dp)
    }
}
