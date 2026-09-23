package com.app.builder.data.resource

import org.jetbrains.compose.resources.DrawableResource
import appbuilder.clientapp.generated.resources.Res
import appbuilder.clientapp.generated.resources.ic_launcher

/**
 * A type-safe registry for image assets, mapping logical resource objects to their physical paths and generated [DrawableResource] references.
 * Ensures UI components reference images via strongly-typed objects rather than raw IDs or paths, facilitating easier maintenance and refactoring.
 *
 * @property url Remote address of the image.
 * @property path The asset's path relative to `commonMain/composeResources`, including the `drawable/` prefix.
 * @property drawable The generated [DrawableResource] reference.
 */
sealed class ImageResource(
    val url: String? = null,
    val path: String? = null,
    val drawable: DrawableResource? = null
) {
    /** The application's launcher icon. */
    data object Launcher: ImageResource(path = "drawable/ic_launcher.png", drawable = Res.drawable.ic_launcher)
    /** CMP logo. */
    data object Kotlin: ImageResource(url = "https://kotlinlang.org/images/compose-multiplatform/hero/compose-multiplatform-logo.svg")
}

