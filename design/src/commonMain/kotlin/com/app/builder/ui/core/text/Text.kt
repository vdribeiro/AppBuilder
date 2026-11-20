package com.app.builder.ui.core.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.Preview
import androidx.compose.material3.Text as MaterialText

/**
 * A standard layout text element that accepts a [TextValue], resolves it into the string to display,
 * and enforces a default [TextOverflow.Ellipsis] overflow strategy for clean truncation layouts.
 *
 * @param modifier The [Modifier] to be applied to the text bounding box layout structure.
 * @param value The text to render on screen.
 * @param textAlign The alignment strategy for the text within its layout boundaries.
 * @param maxLines The maximum vertical lines allowed before truncating content layout blocks.
 * @param style Typography styling configurations applied to the text. Defaults to [LocalTextStyle].
 * @param color The specific color applied to the text layer. Defaults to [Color.Unspecified].
 * @param fontWeight The stroke thickness rendering weight applied to the font glyphs.
 * @param selectable Whether the text can be selected and copied.
 */
@Composable
fun Text(
    modifier: Modifier = Modifier,
    value: TextValue,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    selectable: Boolean = false,
) {
    val content = @Composable {
        MaterialText(
            modifier = modifier,
            text = value.resolve(),
            textAlign = textAlign,
            maxLines = maxLines,
            style = style,
            color = color,
            fontWeight = fontWeight,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (selectable) SelectionContainer(content = content) else content()
}

/**
 * A standard layout text element that accepts a standard [String], shown as a [TextValue.Translation] when [translate] and as a [TextValue.Plain] otherwise.
 *
 * @param modifier The [Modifier] to be applied to the text bounding box layout structure.
 * @param text The literal text string content to render on screen.
 * @param translate True to try to fetch the translation value from the cache, false to show the plain given [text]. Defaults to true.
 * @param textAlign The alignment strategy for the text within its layout boundaries.
 * @param maxLines The maximum vertical lines allowed before truncating content layout blocks.
 * @param style Typography styling configurations applied to the text. Defaults to [LocalTextStyle].
 * @param color The specific color applied to the text layer. Defaults to [Color.Unspecified].
 * @param fontWeight The stroke thickness rendering weight applied to the font glyphs.
 * @param selectable Whether the text can be selected and copied.
 */
@Composable
fun Text(
    modifier: Modifier = Modifier,
    text: String = "",
    translate: Boolean = true,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    selectable: Boolean = false,
) = Text(
    modifier = modifier,
    value = text.toTextValue(translate = translate),
    textAlign = textAlign,
    maxLines = maxLines,
    style = style,
    color = color,
    fontWeight = fontWeight,
    selectable = selectable
)

/**
 * A styled layout text element that accepts an [AnnotatedString], enabling rich textual content structures with multi-style spans, embedded inline modifications and hyperlinks.
 *
 * @param modifier The [Modifier] to be applied to the text bounding box layout structure.
 * @param text The rich text buffer container with compiled style span metadata. If null, displays an empty block.
 * @param textAlign The alignment strategy for the text within its layout boundaries.
 * @param maxLines The maximum vertical lines allowed before truncating content layout blocks.
 * @param style Typography styling configurations applied to the text. Defaults to [LocalTextStyle].
 * @param color The specific color applied to the text layer. Defaults to [Color.Unspecified].
 * @param fontWeight The stroke thickness rendering weight applied to the font glyphs.
 */
@Composable
fun Text(
    modifier: Modifier = Modifier,
    text: AnnotatedString? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
) {
    MaterialText(
        modifier = modifier,
        text = text ?: AnnotatedString(text = ""),
        textAlign = textAlign,
        maxLines = maxLines,
        style = style,
        color = color,
        fontWeight = fontWeight,
    )
}

@PreviewLightDark
@Composable
private fun TextPreview() = Preview {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        val colorScheme = LocalColorScheme.current
        Text(text = "Text")
        Text(text = AnnotatedString(text = "Text"))
        Text(
            text = "Text",
            textAlign = TextAlign.End
        )
        Text(
            text = "Text",
            maxLines = 1,
            color = colorScheme.primary
        )
        Text(
            text = "Text",
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Text",
            selectable = true
        )
    }
}
