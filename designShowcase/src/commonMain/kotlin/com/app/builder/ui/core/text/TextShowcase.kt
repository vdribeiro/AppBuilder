package com.app.builder.ui.core.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.builder.ShowcaseSection
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.core.image.Icon

/** Displays text styles, text inputs, and search bars in both inline and full-screen forms. */
@Composable
fun TextShowcase() {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 32.dp),
    ) {
        ShowcaseSection(title = "Text") { TextPreview() }
        ShowcaseSection(title = "Input") { InputPreview() }
        ShowcaseSection(title = "Search Bar") { SearchBarPreview() }
        ShowcaseSection(title = "Full Screen Search Bar") { SearchBarFullScreenPreview() }
    }
}

@Composable
private fun TextPreview() {
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
    }
}

@Composable
private fun InputPreview() {
    var value by remember { mutableStateOf(value = "") }
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Input(
            enabled = true,
            value = value,
            onValueChange = { value = it },
            leadingIcon = { Icon(imageVector = Icons.Default.Search) }
        )
        Input(
            enabled = true,
            value = value,
            onValueChange = { value = it },
            maxLines = 10
        )
        Input(
            enabled = false,
            value = value,
            onValueChange = { value = it },
        )
        Input(
            isError = true,
            value = value,
            onValueChange = { value = it },
        )
        Input(
            enabled = true,
            value = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Etiam eget ligula eu lectus lobortis condimentum. " +
                    "Aliquam nonummy auctor massa. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. " +
                    "Nulla at risus. Quisque purus magna, auctor et, sagittis ac, posuere eu, lectus. Nam mattis, felis ut adipiscing.",
            maxLines = 1
        )
    }
}

@Composable
private fun SearchBarPreview() {
    var query by remember { mutableStateOf(value = "") }
    var expanded by remember { mutableStateOf(value = false) }
    val allItems = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")

    SearchBar(
        modifier = Modifier.fillMaxWidth(),
        query = query,
        onQueryChange = { query = it },
        onSearch = { expanded = false },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        placeholder = { Text(text = "Search fruit") },
        results = {
            Column {
                allItems.filter { it.contains(other = query, ignoreCase = true) }.forEach { item ->
                    Text(text = item, modifier = Modifier.fillMaxWidth().padding(all = 12.dp))
                }
            }
        }
    )
}

@Composable
private fun SearchBarFullScreenPreview() {
    var query by remember { mutableStateOf(value = "") }
    var expanded by remember { mutableStateOf(value = false) }
    val allItems = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry")

    Column(modifier = Modifier.height(height = 330.dp), verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        SearchBar(
            modifier = Modifier.fillMaxWidth(),
            query = query,
            onQueryChange = { query = it },
            onSearch = { expanded = false },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            placeholder = { Text(text = "Search fruit") },
            fullScreen = true,
            results = {
                Column {
                    allItems.filter { it.contains(other = query, ignoreCase = true) }.forEach { item ->
                        Text(text = item, modifier = Modifier.fillMaxWidth().padding(all = 12.dp))
                    }
                }
            }
        )
        Text(modifier = Modifier.padding(top = 16.dp), text = "Sample Content")
    }
}
