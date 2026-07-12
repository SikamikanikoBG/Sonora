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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import coil.compose.SubcomposeAsyncImage
import com.sikamikaniko.sonora.data.Album
import com.sikamikaniko.sonora.data.Song
import com.sikamikaniko.sonora.data.Subsonic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: SonoraViewModel, modifier: Modifier = Modifier) {
    val albums by vm.albums.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val searchResult by vm.searchResult.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Sonora") },
            actions = {
                IconButton(onClick = { vm.loadLibrary() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { vm.logout() }) {
                    Icon(Icons.Filled.Logout, contentDescription = "Log out")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                vm.search(it)
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Search songs, albums, artists") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )

        when {
            loading && albums.isEmpty() -> CenterBox { CircularProgressIndicator() }
            error != null && albums.isEmpty() -> CenterBox {
                Text(error ?: "Error", color = MaterialTheme.colorScheme.error)
            }
            query.isNotBlank() -> SearchResults(vm, searchResult)
            else -> AlbumGrid(albums, onOpen = { vm.openAlbum(it.id) })
        }
    }
}

@Composable
private fun AlbumGrid(albums: List<Album>, onOpen: (Album) -> Unit) {
    if (albums.isEmpty()) {
        CenterBox { Text("No albums yet. Add music to your server.") }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 158.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        gridItems(albums, key = { it.id }) { album ->
            AlbumCard(album, onClick = { onOpen(album) })
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        CoverFill(album.coverArt)
        Spacer(Modifier.height(8.dp))
        Text(
            album.name ?: "Unknown album",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            album.artist ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CoverFill(coverId: String?) {
    val url = Subsonic.coverArtUrl(coverId, 512)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url == null) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                error = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
        }
    }
}

@Composable
private fun SearchResults(vm: SonoraViewModel, result: com.sikamikaniko.sonora.data.SearchResult3?) {
    val songs = result?.song ?: emptyList()
    val albums = result?.album ?: emptyList()
    if (songs.isEmpty() && albums.isEmpty()) {
        CenterBox { Text("No matches") }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (albums.isNotEmpty()) {
            item { SectionHeader("Albums") }
            items(albums, key = { "al_" + it.id }) { album ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.openAlbum(album.id) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Artwork(album.coverArt, size = 48.dp)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(album.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(album.artist ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (songs.isNotEmpty()) {
            item { SectionHeader("Songs") }
            itemsIndexed(songs) { index, song ->
                SongRow(song, index + 1) { vm.playSongs(songs, index) }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 14.dp, top = 14.dp, bottom = 6.dp)
    )
}

@Composable
fun SongRow(song: Song, index: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            "$index",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title ?: "Unknown", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                song.artist ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.size(8.dp))
        Text(
            formatSeconds(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
