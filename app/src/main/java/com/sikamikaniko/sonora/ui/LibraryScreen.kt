package com.sikamikaniko.sonora.ui

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sikamikaniko.sonora.data.Artist
import com.sikamikaniko.sonora.data.Genre

private val SORTS = listOf(
    "Name" to "alphabeticalByName",
    "Artist" to "alphabeticalByArtist",
    "Recently added" to "newest",
    "Recently played" to "recent",
    "Most played" to "frequent"
)

@Composable
fun LibraryScreen(vm: SonoraViewModel, nav: NavController) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Albums", "Artists", "Genres", "Favourites")

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            "Library",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 14.dp)
        )
        SegmentedTabs(titles = titles, selected = tab, onSelect = { tab = it })
        Spacer(Modifier.height(8.dp))
        Crossfade(
            targetState = tab,
            animationSpec = tween(220),
            label = "libraryTab",
            modifier = Modifier.fillMaxSize()
        ) { t ->
            when (t) {
                0 -> AlbumsTab(vm, nav)
                1 -> ArtistsTab(vm, nav)
                2 -> GenresTab(vm, nav)
                else -> FavouritesTab(vm, nav)
            }
        }
    }
}

/** iOS-style segmented control with a smoothly sliding brand-gradient pill. */
@Composable
private fun SegmentedTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val brand = LocalBrandBrush.current
    val animIndex by animateFloatAsState(
        targetValue = selected.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillOffset"
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(5.dp)
    ) {
        val count = titles.size.coerceAtLeast(1)
        val pillWidth = maxWidth / count
        // Sliding selection pill
        Box(
            Modifier
                .width(pillWidth)
                .fillMaxHeight()
                .offset(x = pillWidth * animIndex)
                .clip(RoundedCornerShape(13.dp))
                .background(brand)
        )
        // Labels
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            titles.forEachIndexed { i, title ->
                val isSelected = i == selected
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "tabColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(13.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = labelColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumsTab(vm: SonoraViewModel, nav: NavController) {
    val albums by vm.albums.collectAsState()
    val sort by vm.albumSort.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp)
        ) {
            Text(
                "${albums.size} albums",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            SortMenu(sort) { vm.setAlbumSort(it) }
        }
        if (albums.isEmpty()) { CenterMessage("No albums yet. Add music to your server."); return@Column }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(158.dp),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            gridItems(albums, key = { it.id }) { album -> AlbumItem(vm, nav, album) }
        }
    }
}

@Composable
private fun SortMenu(current: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = SORTS.firstOrNull { it.second == current }?.first ?: "Sort"
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(label)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SORTS.forEach { (label, type) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { open = false; onPick(type) })
            }
        }
    }
}

@Composable
private fun ArtistsTab(vm: SonoraViewModel, nav: NavController) {
    val artists by vm.artists.collectAsState()
    if (artists.isEmpty()) { CenterMessage("No artists found."); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(artists, key = { it.id }) { artist ->
            ArtistRow(artist) { nav.navigate("artist/${artist.id}") }
        }
    }
}

@Composable
fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    val name = artist.name ?: "Unknown"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        MonogramAvatar(label = name, gradient = true)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val count = artist.albumCount ?: 0
            if (count > 0) Text(
                "$count album${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RowChevron()
    }
}

@Composable
private fun GenresTab(vm: SonoraViewModel, nav: NavController) {
    val genres by vm.genres.collectAsState()
    if (genres.isEmpty()) { CenterMessage("No genres found."); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(genres, key = { it.value ?: "" }) { genre ->
            GenreRow(genre) { genre.value?.let { nav.navigate("genre/${Uri.encode(it)}") } }
        }
    }
}

@Composable
private fun GenreRow(genre: Genre, onClick: () -> Unit) {
    val name = genre.value ?: "Unknown"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        MonogramAvatar(label = name, gradient = false)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val a = genre.albumCount ?: 0
            Text(
                "$a album${if (a == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RowChevron()
    }
}

/** A leading circular avatar: brand-gradient (artists) or tinted surface (genres) with a monogram. */
@Composable
private fun MonogramAvatar(label: String, gradient: Boolean, size: Dp = 46.dp) {
    val brand = LocalBrandBrush.current
    val letter = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val base = Modifier
        .size(size)
        .clip(CircleShape)
    Box(
        modifier = if (gradient) base.background(brand)
        else base.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (gradient) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RowChevron() {
    Icon(
        Icons.Filled.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun FavouritesTab(vm: SonoraViewModel, nav: NavController) {
    val songs by vm.starredSongs.collectAsState()
    val albums by vm.starredAlbums.collectAsState()
    if (songs.isEmpty() && albums.isEmpty()) {
        CenterMessage("No favourites yet. Tap the heart on any song to save it here.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        if (albums.isNotEmpty()) {
            item { SectionHeader("Albums") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(albums, key = { it.id }) { album -> AlbumRailItem(vm, nav, album) }
                }
            }
        }
        if (songs.isNotEmpty()) {
            item { PlayAllHeader("Songs", onPlay = { vm.playSongs(songs, 0) }, onShuffle = { vm.shufflePlay(songs) }) }
            itemsIndexed(songs) { index, song -> SongItem(vm, nav, song, songs, index, showIndex = false) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
