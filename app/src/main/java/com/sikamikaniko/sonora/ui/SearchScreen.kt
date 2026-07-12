package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SearchScreen(vm: SonoraViewModel, nav: NavController) {
    val result by vm.searchResult.collectAsState()
    val starred by vm.starredIds.collectAsState()
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; vm.search(it) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; vm.search("") }) { Icon(Icons.Filled.Clear, "Clear") }
                }
            },
            placeholder = { Text("Artists, albums, songs") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        val artists = result?.artist ?: emptyList()
        val albums = result?.album ?: emptyList()
        val songs = result?.song ?: emptyList()

        if (query.isBlank()) {
            CenterMessage("Search your whole library — artists, albums and songs.")
            return
        }
        if (artists.isEmpty() && albums.isEmpty() && songs.isEmpty()) {
            CenterMessage("No matches for \"$query\".")
            return
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (artists.isNotEmpty()) {
                item { SectionHeader("Artists") }
                items(artists, key = { "ar_" + it.id }) { artist ->
                    ArtistRow(artist) { nav.navigate("artist/${artist.id}") }
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionHeader("Albums") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(albums, key = { "al_" + it.id }) { album ->
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
}
