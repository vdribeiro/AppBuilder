package com.app.builder.ui.component.image

import com.app.builder.data.resource.ImageResource
import com.app.builder.ui.core.image.Image

fun ImageResource.toImage(): Image = Image(
    url = url,
    path = path,
    drawable = drawable
)