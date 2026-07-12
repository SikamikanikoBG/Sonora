package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sikamikaniko.sonora.data.Album

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: SonoraViewModel, nav: NavController) {
    val newest by vm.newest.collectAsState()
    val recent by vm.recent.collectAsState()
    val frequent by vm.frequent.collectAsState()
    val random by vm.randomAlbums.collectAsState()

    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Sonora") },
            actions = {
                IconButton(onClick = { vm.shuffleLibrary() }) { Icon(Icons.Filled.Shuffle, "Shuffle all") }
                IconButton(onClick = { vm.refreshAll() }) { Icon(Icons.Filled.Refresh, "Refresh") }
                IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Filled.Settings, "Settings") }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item { Rail("Recently added", newest, vm, nav) }
            item { Rail("Recently played", recent, vm, nav) }
            item { Rail("Most played", frequent, vm, nav) }
            item { Rail("Discover", random, vm, nav) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Rail(title: String, albums: List<Album>, vm: SonoraViewModel, nav: NavController) {
    if (albums.isEmpty()) return
    SectionHeader(title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumRailItem(vm, nav, album)
        }
    }
}
