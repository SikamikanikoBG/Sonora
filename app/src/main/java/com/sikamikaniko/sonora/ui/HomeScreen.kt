package com.sikamikaniko.sonora.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.text.style.TextOverflow
import com.sikamikaniko.sonora.data.AiMix
import com.sikamikaniko.sonora.data.Album
import java.time.LocalTime

@Composable
fun HomeScreen(vm: SonoraViewModel, nav: NavController) {
    val newest by vm.newest.collectAsState()
    val recent by vm.recent.collectAsState()
    val frequent by vm.frequent.collectAsState()
    val random by vm.randomAlbums.collectAsState()
    val mixes by vm.mixes.collectAsState()
    val brand = LocalBrandBrush.current
    val context = LocalContext.current
    var renaming by remember { mutableStateOf<AiMix?>(null) }

    renaming?.let { mix ->
        var name by remember(mix.id) { mutableStateOf(mix.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename mix") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { vm.renameMix(mix.id, name); renaming = null }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }

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
        if (mixes.isNotEmpty()) {
            item {
                SectionHeader("Your AI mixes")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mixes, key = { it.id }) { mix ->
                        MixCard(
                            mix, brand,
                            onPlay = { vm.playMix(mix) },
                            onRename = { renaming = mix },
                            onDelete = { vm.deleteMix(mix.id) },
                            onShare = { ShareUtil.shareMix(context, mix.name, mix.criteria) }
                        )
                    }
                    item { NewMixCard { nav.navigate("ai") } }
                }
            }
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

private fun mixIcon(criteria: String): ImageVector {
    val t = criteria.lowercase()
    return when {
        listOf("cod", "program", "dev").any { t.contains(it) } -> Icons.Filled.Code
        listOf("gym", "workout", "run", "rage", "hype", "energy").any { t.contains(it) } -> Icons.Filled.FitnessCenter
        listOf("study", "focus", "concentrat").any { t.contains(it) } -> Icons.Filled.MenuBook
        listOf("chill", "relax", "calm", "mellow").any { t.contains(it) } -> Icons.Filled.DarkMode
        listOf("party", "dance", "club").any { t.contains(it) } -> Icons.Filled.Celebration
        listOf("love", "romance").any { t.contains(it) } -> Icons.Filled.Favorite
        listOf("sleep", "night", "ambient").any { t.contains(it) } -> Icons.Filled.Bedtime
        listOf("jazz", "classic", "orchestra", "piano").any { t.contains(it) } -> Icons.Filled.MusicNote
        else -> Icons.Filled.GraphicEq
    }
}

/** Compact, flat mix card — small gradient icon tile + name, in a horizontal row. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MixCard(
    mix: AiMix,
    brand: androidx.compose.ui.graphics.Brush,
    onPlay: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .width(184.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(onClick = onPlay, onLongClick = { menu = true })
                .padding(10.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(brand),
                contentAlignment = Alignment.Center
            ) {
                Icon(mixIcon(mix.criteria), null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                mix.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Play") }, onClick = { menu = false; onPlay() })
            DropdownMenuItem(text = { Text("Share") }, leadingIcon = { Icon(Icons.Filled.Share, null) }, onClick = { menu = false; onShare() })
            DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
        }
    }
}

@Composable
private fun NewMixCard(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Text("New mix", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
