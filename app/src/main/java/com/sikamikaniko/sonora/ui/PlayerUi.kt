package com.sikamikaniko.sonora.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.SubcomposeAsyncImage

// ---------------------------------------------------------------------------
// Mini player — a tactile bar that floats above the nav, with a live progress
// hairline drawn in the theme's brand gradient.
// ---------------------------------------------------------------------------

@Composable
fun MiniPlayer(vm: SonoraViewModel, onExpand: () -> Unit) {
    val title by vm.title.collectAsState()
    val artist by vm.artist.collectAsState()
    val artwork by vm.artworkUri.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val position by vm.position.collectAsState()
    val duration by vm.duration.collectAsState()
    val brand = LocalBrandBrush.current

    val progress = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(progress, tween(280), label = "miniProgress")

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.98f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "miniScale"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth().scale(scale)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Live progress hairline in brand gradient.
            Box(
                Modifier.fillMaxWidth().height(2.5.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).background(brand)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onExpand)
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                UrlArt(artwork, Modifier.size(48.dp), corner = 12.dp)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title ?: "Not playing",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        artist ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.togglePlay() }) {
                    Crossfade(targetState = isPlaying, label = "miniPlayIcon") { playing ->
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play/Pause",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = { vm.next() }) {
                    Icon(Icons.Filled.SkipNext, "Next", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Now Playing — immersive art-derived backdrop, breathing cover, brand-gradient
// transport FAB, refined seek bar and clearly-tinted controls.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
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

    val brand = LocalBrandBrush.current
    val haptics = LocalHapticFeedback.current

    val isStarred = mediaId != null && starredIds.contains(mediaId)
    var dragging by remember { mutableStateOf(false) }
    var dragPos by remember { mutableFloatStateOf(0f) }
    val sliderMax = duration.toFloat().coerceAtLeast(1f)
    val sliderValue = (if (dragging) dragPos else position.toFloat()).coerceIn(0f, sliderMax)

    // Cover breathing: gently shrinks when paused, softly pulses when playing.
    val inf = rememberInfiniteTransition(label = "npPulse")
    val pulse by inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "coverPulse"
    )
    val settle by animateFloatAsState(
        if (isPlaying) 1f else 0.9f,
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "coverSettle"
    )
    val artScale = settle * (if (isPlaying) pulse else 1f)

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

            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.fillMaxWidth().aspectRatio(1f).padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 26.dp,
                    modifier = Modifier.fillMaxSize().scale(artScale)
                ) {
                    UrlArt(artwork, Modifier.fillMaxSize(), corner = 28.dp)
                }
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title ?: "Not playing",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        artist ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                mediaId?.let { id ->
                    val heartScale by animateFloatAsState(
                        if (isStarred) 1.18f else 1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "heart"
                    )
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.toggleStar(id)
                    }) {
                        Icon(
                            if (isStarred) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Favourite",
                            tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.scale(heartScale)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Slider(
                value = sliderValue,
                onValueChange = { dragging = true; dragPos = it },
                onValueChangeFinished = { vm.seekTo(dragPos.toLong()); dragging = false },
                valueRange = 0f..sliderMax,
                thumb = {
                    val thumbScale by animateFloatAsState(if (dragging) 1.3f else 1f, label = "thumb")
                    Box(
                        Modifier.size(18.dp).scale(thumbScale)
                            .clip(CircleShape).background(brand)
                            .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                },
                track = {
                    val frac = (sliderValue / sliderMax).coerceIn(0f, 1f)
                    Box(
                        Modifier.fillMaxWidth().height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(CircleShape).background(brand))
                    }
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(sliderValue.toLong()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        "Shuffle",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { vm.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(40.dp))
                }

                // Signature brand-gradient transport FAB.
                val fabInteraction = remember { MutableInteractionSource() }
                val fabPressed by fabInteraction.collectIsPressedAsState()
                val fabScale by animateFloatAsState(
                    if (fabPressed) 0.92f else 1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "fabScale"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(78.dp).scale(fabScale)
                        .clip(CircleShape).background(brand)
                        .clickable(interactionSource = fabInteraction, indication = LocalIndication.current) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.togglePlay()
                        }
                ) {
                    Crossfade(targetState = isPlaying, label = "fabIcon") { playing ->
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(onClick = { vm.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipNext, "Next", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        "Repeat",
                        tint = if (repeatMode == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.clickable(onClick = onOpenQueue)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, null, tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Text("Up next", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (sleepLeft > 0) {
                Spacer(Modifier.height(10.dp))
                Text("Sleep in $sleepLeft min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
        }
      }
    }
}

// ---------------------------------------------------------------------------
// Backdrop — blurred artwork + drifting brand shimmer + legibility scrims.
// Blur only applies on API 31+, so the base scrim is always present.
// ---------------------------------------------------------------------------

@Composable
private fun Backdrop(artwork: String?) {
    val brand = LocalBrandBrush.current
    val inf = rememberInfiniteTransition(label = "backdrop")
    val shimmer by inf.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmer"
    )
    val hasArt = !artwork.isNullOrBlank()
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (hasArt) {
            SubcomposeAsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(48.dp)
            )
        }
        // Base scrim: keeps content legible and makes the blur read well below API 31.
        Box(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.background.copy(alpha = if (hasArt) 0.72f else 1f)
            )
        )
        // Slow-drifting brand wash.
        Box(Modifier.fillMaxSize().alpha(shimmer).background(brand))
        // Bottom fade so the transport controls always sit on solid ground.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                )
            )
        )
    }
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
