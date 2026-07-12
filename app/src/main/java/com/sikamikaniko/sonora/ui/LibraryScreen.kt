package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sikamikaniko.sonora.data.Artist

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: SonoraViewModel, nav: NavController) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Albums", "Artists", "Favourites")

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
            else -> FavouritesTab(vm, nav)
        }
    }
}

@Composable
private fun AlbumsTab(vm: SonoraViewModel, nav: NavController) {
    val albums by vm.albums.collectAsState()
    if (albums.isEmpty()) { CenterMessage("No albums yet. Add music to your server."); return }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(158.dp),
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(albums, key = { it.id }) { album ->
            AlbumGridCard(album) { nav.navigate("album/${album.id}") }
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
private fun FavouritesTab(vm: SonoraViewModel, nav: NavController) {
    val songs by vm.starredSongs.collectAsState()
    val albums by vm.starredAlbums.collectAsState()
    val starred by vm.starredIds.collectAsState()
    if (songs.isEmpty() && albums.isEmpty()) {
        CenterMessage("No favourites yet. Tap the heart on any song to save it here.")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        if (albums.isNotEmpty()) {
            item { SectionHeader("Albums") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(albums, key = { it.id }) { album ->
                        AlbumRailCard(album) { nav.navigate("album/${album.id}") }
                    }
                }
            }
        }
        if (songs.isNotEmpty()) {
            item { SectionHeader("Songs") }
            itemsIndexed(songs) { index, song ->
                SongRow(
                    song = song,
                    starred = starred.contains(song.id),
                    onToggleStar = { vm.toggleStar(song.id) },
                    onClick = { vm.playSongs(songs, index) }
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
