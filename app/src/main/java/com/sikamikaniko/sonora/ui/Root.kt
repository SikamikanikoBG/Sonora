package com.sikamikaniko.sonora.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val loggedIn by vm.loggedIn.collectAsState()
    if (!loggedIn) {
        LoginScreen(vm)
        return
    }

    val nav = rememberNavController()
    val hasCurrent by vm.hasCurrent.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    val tabs = listOf(Tab.Home, Tab.Library, Tab.Search, Tab.Playlists)

    Scaffold(
        bottomBar = {
            Column {
                if (hasCurrent) MiniPlayer(vm, onExpand = { showPlayer = true })
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
                AlbumScreen(vm, entry.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() })
            }
            composable("artist/{id}") { entry ->
                ArtistScreen(vm, entry.arguments?.getString("id") ?: "", nav)
            }
            composable("playlist/{id}") { entry ->
                PlaylistScreen(vm, entry.arguments?.getString("id") ?: "", nav)
            }
        }
    }

    if (showPlayer && hasCurrent) {
        NowPlayingScreen(vm, onBack = { showPlayer = false }, onOpenQueue = { showQueue = true })
    }
    if (showQueue) {
        QueueScreen(vm, onBack = { showQueue = false })
    }
    BackHandler(enabled = showPlayer || showQueue) {
        when {
            showQueue -> showQueue = false
            showPlayer -> showPlayer = false
        }
    }
}
