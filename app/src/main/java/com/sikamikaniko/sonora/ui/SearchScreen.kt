package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(vm: SonoraViewModel, nav: NavController) {
    val result by vm.searchResult.collectAsState()
    val recent by vm.recentSearches.collectAsState()
    var query by remember { mutableStateOf("") }

    fun go(q: String) { query = q; vm.search(q) }
    fun commit(q: String) { if (q.isNotBlank()) vm.addRecentSearch(q) }

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
            keyboardActions = KeyboardActions(onSearch = { commit(query) }),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        val artists = result?.artist ?: emptyList()
        val albums = result?.album ?: emptyList()
        val songs = result?.song ?: emptyList()

        // Empty query -> recent searches
        if (query.isBlank()) {
            if (recent.isEmpty()) {
                CenterMessage("Search your whole library — artists, albums and songs.")
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Recent", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text("Clear", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { vm.clearRecentSearches() })
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recent.forEach { r -> Chip(r) { go(r) } }
                    }
                }
            }
            return
        }

        // Type-ahead suggestions (recent + matching names)
        val q = query.trim()
        val suggestions = remember(q, recent, result) {
            val set = LinkedHashSet<String>()
            recent.filter { it.contains(q, true) && !it.equals(q, true) }.forEach { set.add(it) }
            artists.mapNotNull { it.name }.filter { it.contains(q, true) && !it.equals(q, true) }.forEach { set.add(it) }
            albums.mapNotNull { it.name }.filter { it.contains(q, true) && !it.equals(q, true) }.forEach { set.add(it) }
            set.take(6)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (suggestions.isNotEmpty()) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        suggestions.forEach { s -> Chip(s) { go(s); commit(s) } }
                    }
                }
            }
            if (artists.isEmpty() && albums.isEmpty() && songs.isEmpty()) {
                item { CenterMessage("No matches for \"$query\".") }
            }
            if (artists.isNotEmpty()) {
                item { SectionHeader("Artists") }
                items(artists, key = { "ar_" + it.id }) { artist ->
                    ArtistRow(artist) { commit(query); nav.navigate("artist/${artist.id}") }
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionHeader("Albums") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(albums, key = { "al_" + it.id }) { album ->
                            AlbumRailItem(vm, nav, album)
                        }
                    }
                }
            }
            if (songs.isNotEmpty()) {
                item {
                    PlayAllHeader(
                        "Songs (${songs.size})",
                        onPlay = { commit(query); vm.playSearchSongs(false) },
                        onShuffle = { commit(query); vm.playSearchSongs(true) }
                    )
                }
                itemsIndexed(songs) { index, song ->
                    SongItem(vm, nav, song, songs, index, showIndex = false)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
