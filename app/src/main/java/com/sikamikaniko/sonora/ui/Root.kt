package com.sikamikaniko.sonora.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Tab("home", "Home", Icons.Filled.Home)
    data object Library : Tab("library", "Library", Icons.Filled.LibraryMusic)
    data object Search : Tab("search", "Search", Icons.Filled.Search)
    data object Playlists : Tab("playlists", "Playlists", Icons.AutoMirrored.Filled.QueueMusic)
}

@Composable
fun SonoraRoot(vm: SonoraViewModel = viewModel()) {
    UpdateDialog(vm)
    PlaylistPickerDialog(vm)
    val loggedIn by vm.loggedIn.collectAsState()
    if (!loggedIn) {
        LoginScreen(vm)
        return
    }

    val nav = rememberNavController()
    val hasCurrent by vm.hasCurrent.collectAsState()
    val selMode by vm.selMode.collectAsState()
    val selectedSongs by vm.selectedSongs.collectAsState()
    val selectedAlbums by vm.selectedAlbums.collectAsState()
    val toast by vm.toast.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showPlayer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }
    val tabs = listOf(Tab.Home, Tab.Library, Tab.Search, Tab.Playlists)
    val selCount = if (selMode == SelMode.SONGS) selectedSongs.size else selectedAlbums.size

    LaunchedEffect(toast) {
        toast?.let { snackbar.showSnackbar(it); vm.consumeToast() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                if (selMode != SelMode.NONE) SelectionBar(selCount, selMode, vm)
                else if (hasCurrent) MiniPlayer(vm, onExpand = { showPlayer = true })
                NavigationBar {
                    val backStack by nav.currentBackStackEntryAsState()
                    val route = backStack?.destination?.route
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Home.route) { HomeScreen(vm, nav) }
            composable(Tab.Library.route) { LibraryScreen(vm, nav) }
            composable(Tab.Search.route) { SearchScreen(vm, nav) }
            composable(Tab.Playlists.route) { PlaylistsScreen(vm, nav) }
            composable("album/{id}") { entry ->
                AlbumScreen(vm, entry.arguments?.getString("id") ?: "", nav, onBack = { nav.popBackStack() })
            }
            composable("artist/{id}") { entry ->
                ArtistScreen(vm, entry.arguments?.getString("id") ?: "", nav)
            }
            composable("playlist/{id}") { entry ->
                PlaylistScreen(vm, entry.arguments?.getString("id") ?: "", nav)
            }
            composable("genre/{name}") { entry ->
                GenreScreen(vm, entry.arguments?.getString("name") ?: "", nav)
            }
            composable("settings") { SettingsScreen(vm, nav) }
            composable("ai") { AskScreen(vm, nav) }
        }
    }

    if (showPlayer && hasCurrent) {
        NowPlayingScreen(
            vm,
            onBack = { showPlayer = false },
            onOpenQueue = { showQueue = true },
            onOpenLyrics = { showLyrics = true },
            onOpenInsights = { showInsights = true },
            onGoToAlbum = { id -> showPlayer = false; nav.navigate("album/$id") },
            onGoToArtist = { id -> showPlayer = false; nav.navigate("artist/$id") }
        )
    }
    if (showQueue) {
        QueueScreen(vm, onBack = { showQueue = false })
    }
    if (showLyrics) {
        LyricsScreen(vm, onBack = { showLyrics = false })
    }
    if (showInsights) {
        LaunchedEffect(Unit) { vm.aiInsights() }
        InsightsScreen(vm, onBack = { showInsights = false; vm.clearAiText() })
    }
    BackHandler(enabled = showPlayer || showQueue || showLyrics || showInsights || selMode != SelMode.NONE) {
        when {
            showInsights -> { showInsights = false; vm.clearAiText() }
            showLyrics -> showLyrics = false
            showQueue -> showQueue = false
            showPlayer -> showPlayer = false
            selMode != SelMode.NONE -> vm.clearSelection()
        }
    }
}

@Composable
private fun UpdateDialog(vm: SonoraViewModel) {
    val update by vm.update.collectAsState()
    val busy by vm.updateBusy.collectAsState()
    val info = update ?: return
    AlertDialog(
        onDismissRequest = { if (!busy) vm.dismissUpdate() },
        title = { Text("Update available") },
        text = { Text("Sonora ${info.version} is available. Update now?") },
        confirmButton = {
            TextButton(onClick = { vm.downloadAndInstallUpdate() }, enabled = !busy) {
                Text(if (busy) "Downloading…" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.dismissUpdate() }, enabled = !busy) { Text("Later") }
        }
    )
}
