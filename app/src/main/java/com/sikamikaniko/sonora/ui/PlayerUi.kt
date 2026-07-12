package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.SubcomposeAsyncImage

@Composable
fun MiniPlayer(vm: SonoraViewModel, onExpand: () -> Unit) {
    val title by vm.title.collectAsState()
    val artist by vm.artist.collectAsState()
    val artwork by vm.artworkUri.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()

    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand).padding(10.dp)
        ) {
            UrlArt(artwork, Modifier.size(46.dp), corner = 8.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title ?: "Not playing", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(artist ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { vm.togglePlay() }) {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause")
            }
            IconButton(onClick = { vm.next() }) { Icon(Icons.Filled.SkipNext, "Next") }
        }
    }
}

@Composable
fun NowPlayingScreen(
    vm: SonoraViewModel,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val title by vm.title.collectAsState()
    val artist by vm.artist.collectAsState()
    val artwork by vm.artworkUri.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val position by vm.position.collectAsState()
    val duration by vm.duration.collectAsState()
    val repeatMode by vm.repeatMode.collectAsState()
    val shuffle by vm.shuffle.collectAsState()
    val mediaId by vm.currentMediaId.collectAsState()
    val starredIds by vm.starredIds.collectAsState()
    val sleepLeft by vm.sleepMinutesLeft.collectAsState()
    val sleepEnd by vm.sleepEndOfTrack.collectAsState()
    val speed by vm.speed.collectAsState()
    val albumId by vm.currentAlbumId.collectAsState()
    val artistId by vm.currentArtistId.collectAsState()

    val isStarred = mediaId != null && starredIds.contains(mediaId)
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(0f) }
    val sliderMax = duration.toFloat().coerceAtLeast(1f)
    val sliderValue = (if (dragging) dragPos else position.toFloat()).coerceIn(0f, sliderMax)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      Box(Modifier.fillMaxSize()) {
        Backdrop(artwork)
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.KeyboardArrowDown, "Collapse") }
                Spacer(Modifier.weight(1f))
                Text("Now Playing", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                SpeedMenu(speed) { vm.setSpeed(it) }
                SleepMenu(
                    sleepLeft, sleepEnd,
                    onPick = { vm.startSleepTimer(it) },
                    onEndOfTrack = { vm.sleepAtEndOfTrack() },
                    onCancel = { vm.cancelSleepTimer() }
                )
                NpOverflow(
                    hasAlbum = albumId != null,
                    hasArtist = artistId != null,
                    onLyrics = onOpenLyrics,
                    onGoAlbum = { albumId?.let(onGoToAlbum) },
                    onGoArtist = { artistId?.let(onGoToArtist) }
                )
            }

            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 8.dp)) {
                UrlArt(artwork, Modifier.fillMaxSize(), corner = 20.dp)
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title ?: "Not playing", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                mediaId?.let { id ->
                    IconButton(onClick = { vm.toggleStar(id) }) {
                        Icon(
                            if (isStarred) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Favourite",
                            tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Slider(
                value = sliderValue,
                onValueChange = { dragging = true; dragPos = it },
                onValueChangeFinished = { vm.seekTo(dragPos.toLong()); dragging = false },
                valueRange = 0f..sliderMax
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(sliderValue.toLong()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.previous() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", modifier = Modifier.size(38.dp))
                }
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp)) {
                    IconButton(onClick = { vm.togglePlay() }) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.SkipNext, "Next", modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        "Repeat",
                        tint = if (repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onOpenQueue).padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(8.dp))
                Text("Up next", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (sleepLeft > 0) {
                Text("Sleep in $sleepLeft min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
      }
    }
}

@Composable
private fun Backdrop(artwork: String?) {
    if (artwork.isNullOrBlank()) return
    SubcomposeAsyncImage(
        model = artwork,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().blur(40.dp)
    )
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.82f)))
}

@Composable
private fun SleepMenu(
    minutesLeft: Int,
    endActive: Boolean,
    onPick: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val active = minutesLeft > 0 || endActive
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Timer, "Sleep timer", tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("End of track") }, onClick = { onEndOfTrack(); expanded = false })
            listOf(15, 30, 45, 60).forEach { m ->
                DropdownMenuItem(text = { Text("$m minutes") }, onClick = { onPick(m); expanded = false })
            }
            if (active) {
                DropdownMenuItem(text = { Text("Cancel timer") }, onClick = { onCancel(); expanded = false })
            }
        }
    }
}

@Composable
private fun SpeedMenu(speed: Float, onPick: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (speed % 1f == 0f) "${speed.toInt()}×" else "$speed×"
    Box {
        TextButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                val l = if (s % 1f == 0f) "${s.toInt()}×" else "$s×"
                DropdownMenuItem(text = { Text(l) }, onClick = { onPick(s); expanded = false })
            }
        }
    }
}

@Composable
private fun NpOverflow(
    hasAlbum: Boolean,
    hasArtist: Boolean,
    onLyrics: () -> Unit,
    onGoAlbum: () -> Unit,
    onGoArtist: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, "More") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Lyrics") }, leadingIcon = { Icon(Icons.Filled.Lyrics, null) }, onClick = { expanded = false; onLyrics() })
            if (hasAlbum) DropdownMenuItem(text = { Text("Go to album") }, leadingIcon = { Icon(Icons.Filled.Album, null) }, onClick = { expanded = false; onGoAlbum() })
            if (hasArtist) DropdownMenuItem(text = { Text("Go to artist") }, leadingIcon = { Icon(Icons.Filled.Person, null) }, onClick = { expanded = false; onGoArtist() })
        }
    }
}
