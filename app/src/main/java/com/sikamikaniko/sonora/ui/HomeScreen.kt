package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sikamikaniko.sonora.data.Album
import java.time.LocalTime

@Composable
fun HomeScreen(vm: SonoraViewModel, nav: NavController) {
    val newest by vm.newest.collectAsState()
    val recent by vm.recent.collectAsState()
    val frequent by vm.frequent.collectAsState()
    val random by vm.randomAlbums.collectAsState()
    val brand = LocalBrandBrush.current

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            HomeHeader(
                brand = brand,
                onAsk = { nav.navigate("ai") },
                onShuffle = { vm.shuffleLibrary() },
                onRefresh = { vm.refreshAll() },
                onSettings = { nav.navigate("settings") }
            )
        }
        item { Rail("Recently added", newest, vm, nav) }
        item { Rail("Recently played", recent, vm, nav) }
        item { Rail("Most played", frequent, vm, nav) }
        item { Rail("Discover", random, vm, nav) }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun HomeHeader(
    brand: androidx.compose.ui.graphics.Brush,
    onAsk: () -> Unit,
    onShuffle: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    val hour = remember { LocalTime.now().hour }
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val onBrand = Color.White
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(brand)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(greeting, style = MaterialTheme.typography.labelLarge, color = onBrand.copy(alpha = 0.85f), modifier = Modifier.weight(1f))
                IconButton(onClick = onAsk) { Icon(Icons.Filled.AutoAwesome, "Ask Sonora", tint = onBrand) }
                IconButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, "Refresh", tint = onBrand) }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, "Settings", tint = onBrand) }
            }
            Text("Sonora", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = onBrand)
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.20f))
                    .clickable(onClick = onShuffle)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Filled.Shuffle, null, tint = onBrand, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Shuffle everything", color = onBrand, fontWeight = FontWeight.SemiBold)
            }
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
