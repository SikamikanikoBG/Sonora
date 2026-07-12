package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.sikamikaniko.sonora.data.Album
import com.sikamikaniko.sonora.data.Song
import com.sikamikaniko.sonora.data.Subsonic

// ---------- artwork ----------

@Composable
fun CoverArt(coverId: String?, modifier: Modifier = Modifier, corner: Dp = 12.dp, requestPx: Int = 512) {
    UrlArt(Subsonic.coverArtUrl(coverId, requestPx), modifier, corner)
}

@Composable
fun UrlArt(url: String?, modifier: Modifier = Modifier, corner: Dp = 12.dp) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            SubcomposeAsyncImage(
                model = url, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                error = { Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }
    }
}

@Composable
fun Artwork(coverId: String?, size: Dp, corner: Dp = 10.dp, requestPx: Int = 512) {
    CoverArt(coverId, Modifier.size(size), corner, requestPx)
}

// ---------- album cards ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumGridCard(
    album: Album,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box {
            CoverArt(album.coverArt, Modifier.fillMaxWidth().aspectRatio(1f))
            if (selectionActive) SelectBadge(selected, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Spacer(Modifier.size(8.dp))
        Text(album.name ?: "Unknown album", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumRailCard(
    album: Album,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.width(148.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Box {
            CoverArt(album.coverArt, Modifier.size(148.dp))
            if (selectionActive) SelectBadge(selected, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
        Spacer(Modifier.size(6.dp))
        Text(album.name ?: "Unknown album", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(album.artist ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AlbumItem(vm: SonoraViewModel, nav: NavController, album: Album) {
    val selMode by vm.selMode.collectAsState()
    val selectedAlbums by vm.selectedAlbums.collectAsState()
    val active = selMode == SelMode.ALBUMS
    val selected = selectedAlbums.any { it.id == album.id }
    AlbumGridCard(
        album, selected, active,
        onClick = { if (active) vm.toggleAlbumSelection(album) else nav.navigate("album/${album.id}") },
        onLongClick = { vm.toggleAlbumSelection(album) }
    )
}

@Composable
fun AlbumRailItem(vm: SonoraViewModel, nav: NavController, album: Album) {
    val selMode by vm.selMode.collectAsState()
    val selectedAlbums by vm.selectedAlbums.collectAsState()
    val active = selMode == SelMode.ALBUMS
    val selected = selectedAlbums.any { it.id == album.id }
    AlbumRailCard(
        album, selected, active,
        onClick = { if (active) vm.toggleAlbumSelection(album) else nav.navigate("album/${album.id}") },
        onLongClick = { vm.toggleAlbumSelection(album) }
    )
}

@Composable
private fun SelectBadge(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(26.dp).clip(RoundedCornerShape(50)).background(Color(0x99000000)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.White
        )
    }
}

// ---------- song row ----------

data class SongRowActions(
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onGoToArtist: (() -> Unit)?,
    val onGoToAlbum: (() -> Unit)?,
    val onToggleStar: () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    index: Int? = null,
    starred: Boolean = false,
    selected: Boolean = false,
    selectionActive: Boolean = false,
    actions: SongRowActions? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (selectionActive) {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(10.dp))
        } else if (index != null) {
            Text("$index", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
            Spacer(Modifier.size(6.dp))
        } else {
            Artwork(song.coverArt, size = 44.dp)
            Spacer(Modifier.size(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title ?: "Unknown", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!selectionActive) {
            if (starred) {
                Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
            }
            if (actions != null) SongMenu(starred, actions)
            else Text(formatSeconds(song.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SongMenu(starred: Boolean, actions: SongRowActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, "More") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Play next") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, null) },
                onClick = { open = false; actions.onPlayNext() }
            )
            DropdownMenuItem(
                text = { Text("Add to queue") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
                onClick = { open = false; actions.onAddToQueue() }
            )
            DropdownMenuItem(
                text = { Text(if (starred) "Remove favourite" else "Favourite") },
                leadingIcon = { Icon(if (starred) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null) },
                onClick = { open = false; actions.onToggleStar() }
            )
            actions.onGoToArtist?.let { go ->
                DropdownMenuItem(
                    text = { Text("Go to artist") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    onClick = { open = false; go() }
                )
            }
            actions.onGoToAlbum?.let { go ->
                DropdownMenuItem(
                    text = { Text("Go to album") },
                    leadingIcon = { Icon(Icons.Filled.Album, null) },
                    onClick = { open = false; go() }
                )
            }
        }
    }
}

@Composable
fun SongItem(vm: SonoraViewModel, nav: NavController, song: Song, songs: List<Song>, index: Int, showIndex: Boolean) {
    val selMode by vm.selMode.collectAsState()
    val selectedSongs by vm.selectedSongs.collectAsState()
    val starredIds by vm.starredIds.collectAsState()
    val active = selMode == SelMode.SONGS
    val selected = selectedSongs.any { it.id == song.id }
    SongRow(
        song = song,
        index = if (showIndex) index + 1 else null,
        starred = starredIds.contains(song.id),
        selected = selected,
        selectionActive = active,
        actions = SongRowActions(
            onPlayNext = { vm.playNext(listOf(song)) },
            onAddToQueue = { vm.addToQueue(listOf(song)) },
            onGoToArtist = song.artistId?.let { aid -> { nav.navigate("artist/$aid") } },
            onGoToAlbum = song.albumId?.let { alid -> { nav.navigate("album/$alid") } },
            onToggleStar = { vm.toggleStar(song.id) }
        ),
        onClick = { if (active) vm.toggleSongSelection(song) else vm.playSongs(songs, index) },
        onLongClick = { vm.toggleSongSelection(song) }
    )
}

// ---------- selection action bar ----------

@Composable
fun SelectionBar(count: Int, mode: SelMode, vm: SonoraViewModel) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
            IconButton(onClick = { vm.clearSelection() }) { Icon(Icons.Filled.Close, "Cancel") }
            Text("$count selected", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            IconButton(onClick = { vm.playSelection(false) }) { Icon(Icons.Filled.PlayArrow, "Play") }
            IconButton(onClick = { vm.playSelection(true) }) { Icon(Icons.Filled.Shuffle, "Shuffle") }
            IconButton(onClick = { vm.playNextSelection() }) { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next") }
            IconButton(onClick = { vm.queueSelection() }) { Icon(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") }
        }
    }
}

// ---------- misc ----------

@Composable
fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
fun PlayAllHeader(title: String, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 2.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        TextButton(onClick = onPlay) {
            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text("Play all")
        }
        IconButton(onClick = onShuffle) { Icon(Icons.Filled.Shuffle, "Shuffle all") }
    }
}

@Composable
fun CenterMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

fun formatSeconds(seconds: Int?): String = formatDuration((seconds ?: 0) * 1000L)
