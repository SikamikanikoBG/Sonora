package com.sikamikaniko.sonora.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: SonoraViewModel, nav: NavController) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Albums", "Artists", "Genres", "Favourites")

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Library") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            titles.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> AlbumsTab(vm, nav)
            1 -> ArtistsTab(vm, nav)
            2 -> GenresTab(vm, nav)
            else -> FavouritesTab(vm, nav)
        }
    }
}

@Composable
private fun AlbumsTab(vm: SonoraViewModel, nav: NavController) {
    val albums by vm.albums.collectAsState()
    val sort by vm.albumSort.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp)) {
            Text("${albums.size} albums", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
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
            Spacer(Modifier.size(4.dp))
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
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.id }) { artist ->
            ArtistRow(artist) { nav.navigate("artist/${artist.id}") }
        }
    }
}

@Composable
fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(Icons.Filled.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(artist.name ?: "Unknown", maxLines = 1, overflow = TextOverflow.Ellipsis)
            val count = artist.albumCount ?: 0
            if (count > 0) Text("$count album${if (count == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GenresTab(vm: SonoraViewModel, nav: NavController) {
    val genres by vm.genres.collectAsState()
    if (genres.isEmpty()) { CenterMessage("No genres found."); return }
    LazyColumn(Modifier.fillMaxSize()) {
        items(genres, key = { it.value ?: "" }) { genre ->
            GenreRow(genre) { genre.value?.let { nav.navigate("genre/${Uri.encode(it)}") } }
        }
    }
}

@Composable
private fun GenreRow(genre: Genre, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(genre.value ?: "Unknown", maxLines = 1, overflow = TextOverflow.Ellipsis)
            val a = genre.albumCount ?: 0
            Text("$a album${if (a == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
