package com.app.builder.ui.screen.nfc

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.nfc.NfcController
import com.app.builder.core.nfc.NfcRecord
import com.app.builder.core.security.toUuid
import com.app.builder.domain.EntityType
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.LocalNfcController
import com.app.builder.ui.Preview
import com.app.builder.ui.component.Store
import com.app.builder.ui.component.bar.ActionBar
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.component.useravatar.UserAvatar
import com.app.builder.ui.component.useravatar.UserAvatarState
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonGroup
import com.app.builder.ui.core.button.ButtonGroupItem
import com.app.builder.ui.core.button.ButtonGroupVariant
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.core.button.Dropdown
import com.app.builder.ui.core.button.DropdownItem
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.divider.Divider
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.overlay.Snackbar
import com.app.builder.ui.core.progress.ProgressIndicator
import com.app.builder.ui.core.text.Input
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.core.list.LazyColumn as ListLazyColumn

/**
 * The Nfc Screen.
 *
 * @param navigationStore Drives the bottom navigation bar.
 * @param userAvatarStore The store for user avatar.
 */
@Composable
fun NfcScreen(
    navigationStore: Store<NavigationState, Unit>,
    userAvatarStore: Store<UserAvatarState, Unit>,
) {
    val nfc = LocalNfcController.current
    val colorScheme = LocalColorScheme.current

    val state by nfc.state.collectAsStateWithLifecycle()

    var snackbarMessage by remember { mutableStateOf<String?>(value = null) }

    var recordKind by remember { mutableStateOf(value = NfcRecordKind.OPEN) }
    var recordKindExpanded by remember { mutableStateOf(value = false) }
    var entityType by remember { mutableStateOf(value = EntityType.TASK) }
    var entityTypeExpanded by remember { mutableStateOf(value = false) }
    var entityUuidText by remember { mutableStateOf(value = "") }

    val available = nfc.available
    val entityUuid = entityUuidText.toUuid()
    val entityUuidError = entityUuidText.isNotBlank() && entityUuid == null
    val canWrite = available && !entityUuidError

    Screen(
        modifier = Modifier.imePadding(),
        bottomBar = { Navigation(store = navigationStore) },
        topBar = { ActionBar(title = "nfc", avatar = { UserAvatar(store = userAvatarStore) }) },
        snackbarHost = {
            snackbarMessage?.let {
                Snackbar(message = it, onDismiss = { snackbarMessage = null })
            }
        }
    ) {
        ListLazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 16.dp)
        ) {
            item(key = "status") {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (available) Icons.Default.Nfc else Icons.Default.Block,
                                tint = if (available) colorScheme.primary else colorScheme.error
                            )
                            Column {
                                Text(text = "Availability", fontWeight = FontWeight.Bold)
                                Text(text = available.toString())
                            }
                        }
                        Divider()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                        ) {
                            if (state != NfcController.State.Idle) ProgressIndicator(modifier = Modifier.size(size = 16.dp))
                            Column {
                                Text(text = "State", fontWeight = FontWeight.Bold)
                                Text(text = state.toString())
                            }
                        }
                    }
                }
            }

            item(key = "scan") {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
                    ) {
                        Text(text = "Scan", fontWeight = FontWeight.Bold)
                        ButtonGroup(
                            variant = ButtonGroupVariant.STANDARD,
                            items = persistentListOf(
                                ButtonGroupItem(
                                    enabled = available && state != NfcController.State.Scanning,
                                    text = "Start Scan",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { nfc.read() }
                                ),
                                ButtonGroupItem(
                                    enabled = state != NfcController.State.Idle,
                                    text = "Stop",
                                    icon = Icons.Default.Stop,
                                    onClick = { nfc.stop() }
                                )
                            )
                        )
                    }
                }
            }

            item(key = "write") {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
                    ) {
                        Text(text = "Write", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                            Column {
                                Button(text = recordKind.name, onClick = { recordKindExpanded = true })
                                Dropdown(
                                    expanded = recordKindExpanded,
                                    onDismissRequest = { recordKindExpanded = false },
                                    items = NfcRecordKind.entries.map { candidate ->
                                        DropdownItem(
                                            text = candidate.name,
                                            onClick = {
                                                recordKindExpanded = false
                                                recordKind = candidate
                                            },
                                            leadingIcon = { if (candidate == recordKind) Icon(imageVector = Icons.Default.Check) }
                                        )
                                    }.toPersistentList()
                                )
                            }
                            Column {
                                Button(text = entityType.name, onClick = { entityTypeExpanded = true })
                                Dropdown(
                                    expanded = entityTypeExpanded,
                                    onDismissRequest = { entityTypeExpanded = false },
                                    items = EntityType.entries.map { candidate ->
                                        DropdownItem(
                                            text = candidate.name,
                                            onClick = {
                                                entityTypeExpanded = false
                                                entityType = candidate
                                            },
                                            leadingIcon = { if (candidate == entityType) Icon(imageVector = Icons.Default.Check) }
                                        )
                                    }.toPersistentList()
                                )
                            }
                        }
                        Input(
                            modifier = Modifier.fillMaxWidth(),
                            value = entityUuidText,
                            onValueChange = { entityUuidText = it },
                            maxLines = 1,
                            isError = entityUuidError
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                            Button(
                                style = ButtonStyle.FILLED,
                                enabled = canWrite,
                                text = "Write Tag",
                                content = { Icon(imageVector = Icons.AutoMirrored.Filled.Send) }
                            ) {
                                val record = when (recordKind) {
                                    NfcRecordKind.OPEN -> NfcRecord.Open(entityType = entityType, entityUuid = entityUuid)
                                    NfcRecordKind.UPSERT -> NfcRecord.Upsert(entityType = entityType, entityUuid = entityUuid)
                                }
                                nfc.write(records = listOf(record))
                            }
                            if (state == NfcController.State.Writing) Button(
                                style = ButtonStyle.TEXT,
                                text = "Cancel Write",
                                content = { Icon(imageVector = Icons.Default.Close) }
                            ) {
                                nfc.stop()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The kind of [NfcRecord] to write. */
private enum class NfcRecordKind { OPEN, UPSERT }

@Preview
@Composable
private fun NfcScreenPreview() = Preview {
    NfcScreen(
        navigationStore = Store(initialState = NavigationState()),
        userAvatarStore = Store(initialState = UserAvatarState(userName = "Nfc")),
    )
}
