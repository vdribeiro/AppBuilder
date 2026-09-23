package com.app.builder.ui.screen.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.builder.core.config.ClientFlags
import com.app.builder.core.media.AudioPlayer
import com.app.builder.data.resource.AudioResource
import com.app.builder.data.resource.AudioResource.Chime
import com.app.builder.data.resource.AudioResource.Drone
import com.app.builder.data.resource.AudioResource.Ping
import com.app.builder.data.resource.AudioResource.Pulse
import com.app.builder.ui.LocalAudioPlayer
import com.app.builder.ui.Preview
import com.app.builder.ui.component.actionbar.ActionBar
import com.app.builder.ui.component.actionbar.ActionBarAction
import com.app.builder.ui.component.actionbar.ActionBarState
import com.app.builder.ui.component.navigation.Navigation
import com.app.builder.ui.component.navigation.NavigationState
import com.app.builder.ui.core.button.Button
import com.app.builder.ui.core.button.ButtonStyle
import com.app.builder.ui.core.button.Checkbox
import com.app.builder.ui.core.button.Switch
import com.app.builder.ui.core.text.Text
import com.app.builder.ui.screen.Screen
import com.app.builder.ui.store.Store

/**
 * The Audio Screen.
 *
 * @param actionBarStore The store for action bar.
 * @param navigationStore Drives the bottom navigation bar.
 */
@Composable
fun AudioScreen(
    actionBarStore: Store<ActionBarState, ActionBarAction>,
    navigationStore: Store<NavigationState, Unit>,
) {
    val audioPlayer = LocalAudioPlayer.current

    val state by audioPlayer.state.collectAsStateWithLifecycle()

    var selected by remember { mutableStateOf(value = setOf<AudioResource>(AudioResource.Ping)) }
    var loop by remember { mutableStateOf(value = true) }
    var shuffle by remember { mutableStateOf(value = false) }
    val entries: List<AudioResource> by lazy { listOf(Ping, Chime, Pulse, Drone) }

    Screen(
        topBar = { ActionBar(store = actionBarStore) },
        bottomBar = { Navigation(store = navigationStore) },
    ) {
        if (!ClientFlags.flags.music || !audioPlayer.available) {
            Text(modifier = Modifier.align(alignment = Alignment.Center), text = "audio_unavailable")
            return@Screen
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Text(text = state.toString(), translate = false)

            Text(text = "audio_tracks")
            entries.forEach { track ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = track in selected,
                        onCheckedChange = { checked -> selected = if (checked) selected + track else selected - track }
                    )
                    Text(
                        text = track.path
                            .substringAfterLast(delimiter = '/')
                            .substringBeforeLast(delimiter = '.')
                            .replace(oldValue = "_", newValue = " ")
                            .replaceFirstChar { it.uppercase() },
                        translate = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(modifier = Modifier.weight(weight = 1f), text = "audio_loop")
                Switch(checked = loop, onCheckedChange = { loop = it })
                Text(modifier = Modifier.weight(weight = 1f), text = "audio_shuffle")
                Switch(checked = shuffle, onCheckedChange = { shuffle = it })
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.FILLED,
                text = "Set playlist",
                enabled = selected.isNotEmpty(),
            ) {
                audioPlayer.setPlaylist(
                    playlist = entries.filter { it in selected }.map { it.path },
                    loop = loop,
                    shuffle = shuffle
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = "Play",
                    enabled = state != AudioPlayer.State.Playing,
                ) {
                    audioPlayer.play()
                }
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = "Pause",
                    enabled = state == AudioPlayer.State.Playing,
                ) {
                    audioPlayer.pause()
                }
                Button(
                    modifier = Modifier.weight(weight = 1f),
                    style = ButtonStyle.FILLED,
                    text = "Stop",
                    enabled = state != AudioPlayer.State.Idle,
                ) {
                    audioPlayer.stop()
                }
            }
        }
    }
}

@Preview
@Composable
private fun AudioScreenPreview() = Preview {
    AudioScreen(
        actionBarStore = Store(initialState = ActionBarState(avatarName = "Audio")),
        navigationStore = Store(initialState = NavigationState()),
    )
}
