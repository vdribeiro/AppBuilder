package com.app.builder.ui.core.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.ui.Preview
import com.app.builder.ui.core.image.Icon
import androidx.compose.material3.SearchBar as MaterialSearchBar

/**
 * A floating search field that expands to reveal search results.
 *
 * @param modifier The [Modifier] to be applied to the search bar's layout.
 * @param query The text buffer string value currently displayed inside the input field.
 * @param onQueryChange Callback dispatched on every keystroke update to [query].
 * @param onSearch Callback invoked when the user submits the current [query].
 * @param expanded Whether the results are currently visible.
 * @param onExpandedChange Callback invoked when the user opens or collapses the results.
 * @param placeholder An optional composable slot shown when [query] is empty.
 * @param fullScreen When `true`, expanding takes over the entire screen instead of showing a bounded panel below the input field. Defaults to `false`.
 * @param results A composable slot rendering the search results shown while [expanded].
 */
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    placeholder: (@Composable () -> Unit)? = null,
    fullScreen: Boolean = false,
    results: @Composable ColumnScope.() -> Unit = {},
) {
    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            modifier = Modifier.fillMaxWidth(),
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            placeholder = placeholder,
            leadingIcon = { Icon(imageVector = Icons.Default.Search) }
        )
    }

    when {
        fullScreen -> MaterialSearchBar(
            modifier = modifier,
            inputField = inputField,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            content = results
        )

        else -> DockedSearchBar(
            modifier = modifier,
            inputField = inputField,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            content = results
        )
    }
}

@Preview
@Composable
private fun SearchBarPreview() = Preview {
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
        fullScreen = false,
        results = {
            Column {
                allItems.filter { it.contains(other = query, ignoreCase = true) }.forEach { item ->
                    Text(text = item, modifier = Modifier.fillMaxWidth().padding(all = 12.dp))
                }
            }
        }
    )
}

@Preview
@Composable
private fun SearchBarFullScreenPreview() = Preview {
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
        fullScreen = true,
        results = {
            Column {
                allItems.filter { it.contains(other = query, ignoreCase = true) }.forEach { item ->
                    Text(text = item, modifier = Modifier.fillMaxWidth().padding(all = 12.dp))
                }
            }
        }
    )
}
