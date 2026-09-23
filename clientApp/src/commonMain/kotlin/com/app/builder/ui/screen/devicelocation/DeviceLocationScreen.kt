package com.app.builder.ui.screen.devicelocation

import kotlin.math.round
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.builder.core.devicelocation.DeviceLocation
import com.app.builder.core.devicelocation.DeviceLocationProvider
import com.app.builder.core.locale.getLocalDateTime
import com.app.builder.core.locale.toInstant
import com.app.builder.ui.LocalColorScheme
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.button.ButtonGroup
import com.app.builder.ui.core.button.ButtonGroupItem
import com.app.builder.ui.core.button.ButtonGroupVariant
import com.app.builder.ui.core.card.Card
import com.app.builder.ui.core.divider.Divider
import com.app.builder.ui.core.image.Icon
import com.app.builder.ui.core.list.LazyColumn
import com.app.builder.ui.core.list.ListItem
import com.app.builder.ui.core.progress.ProgressIndicator
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.devicelocation.LocalDeviceLocationProvider
import com.app.builder.ui.permission.LocalPermissionManager
import com.app.builder.ui.permission.Permission
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Device Location Screen.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 */
@Composable
fun DeviceLocationScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>
) {
    val permissionManager = LocalPermissionManager.current
    val colorScheme = LocalColorScheme.current
    val coroutineScope = rememberCoroutineScope()
    val deviceLocationProvider: DeviceLocationProvider = LocalDeviceLocationProvider.current

    val locations = remember { mutableStateListOf<DeviceLocation>() }
    // TODO - get from repositories

    val available = true
    val active = true

    Screen(
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) }
    ) {
        LazyColumn(
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
                                imageVector = if (available) Icons.Default.LocationOn else Icons.Default.LocationOff,
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
                            if (active) ProgressIndicator(modifier = Modifier.size(size = 16.dp))
                            Column {
                                Text(text = "State", fontWeight = FontWeight.Bold)
                                Text(text = active.toString())
                            }
                        }
                    }
                }
            }

            item(key = "controls") {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
                    ) {
                        Text(text = "Controls", fontWeight = FontWeight.Bold)
                        ButtonGroup(
                            variant = ButtonGroupVariant.STANDARD,
                            items = persistentListOf(
                                ButtonGroupItem(
                                    enabled = available && !active,
                                    text = "Start",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = {
                                        coroutineScope.launch {
                                            if (permissionManager.grantPermission(permission = Permission.LOCATION)) {
                                                deviceLocationProvider.startUpdate()
                                            }
                                        }
                                    }
                                ),
                                ButtonGroupItem(
                                    enabled = active,
                                    text = "Stop",
                                    icon = Icons.Default.Stop,
                                    onClick = { deviceLocationProvider.stopUpdate() }
                                )
                            )
                        )
                    }
                }
            }

            item(key = "locations_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.History)
                    Text(text = "Location History", fontWeight = FontWeight.Bold)
                }
            }

            if (locations.isEmpty()) {
                item(key = "locations_empty") {
                    Text(text = "No locations captured yet")
                }
            } else {
                itemsIndexed(items = locations, key = { _, fix -> fix.uuid }) { index, fix ->
                    val details = listOfNotNull(
                        fix.provider,
                        "accuracy: ±${fix.accuracy.toInt()}m",
                        fix.altitude?.let { "altitude: ${it.roundTo(decimals = 1)}m" },
                        fix.bearing?.let { "bearing: ${it.roundTo(decimals = 0).toInt()}°" },
                        fix.speed?.let { "speed: ${it.roundTo(decimals = 1)}m/s" },
                        fix.fixTime.toInstant()?.toString()?.let { "time: ${getLocalDateTime(utc = it)}" },
                    )
                    ListItem(
                        leadingContent = { Icon(imageVector = Icons.Default.LocationOn) },
                        headlineText = "${fix.latitude.roundTo(decimals = 6)}, ${fix.longitude.roundTo(decimals = 6)}",
                        supportingText = details.joinToString(separator = "\n")
                    )
                    if (index < locations.lastIndex) Divider()
                }
            }
        }
    }
}

/**
 * Rounds this value to a fixed number of decimal places.
 *
 * @param decimals How many decimal places to keep.
 * @return This value rounded to [decimals] decimal places.
 */
private fun Double.roundTo(decimals: Int): Double {
    var factor = 1.0
    repeat(times = decimals) { factor *= 10.0 }
    return round(this * factor) / factor
}

@Preview
@Composable
private fun DeviceLocationScreenPreview() = Preview {
    DeviceLocationScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Device Locations")),
        navigationStore = Store(initialState = NavigationState()),
    )
}
