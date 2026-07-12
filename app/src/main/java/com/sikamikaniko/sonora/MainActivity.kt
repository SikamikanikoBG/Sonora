package com.sikamikaniko.sonora

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sikamikaniko.sonora.ui.AlbumScreen
import com.sikamikaniko.sonora.ui.LibraryScreen
import com.sikamikaniko.sonora.ui.LoginScreen
import com.sikamikaniko.sonora.ui.MiniPlayer
import com.sikamikaniko.sonora.ui.NowPlayingScreen
import com.sikamikaniko.sonora.ui.SonoraTheme
import com.sikamikaniko.sonora.ui.SonoraViewModel

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            SonoraTheme {
                SonoraRoot()
            }
        }
    }
}

@Composable
fun SonoraRoot(vm: SonoraViewModel = viewModel()) {
    val loggedIn by vm.loggedIn.collectAsState()

    if (!loggedIn) {
        LoginScreen(vm)
        return
    }

    val hasCurrent by vm.hasCurrent.collectAsState()
    val currentAlbum by vm.currentAlbum.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (hasCurrent) MiniPlayer(vm, onExpand = { showPlayer = true })
        }
    ) { padding ->
        LibraryScreen(vm, modifier = Modifier.padding(padding))
    }

    currentAlbum?.let { album ->
        AlbumScreen(vm, album, onBack = { vm.clearAlbum() })
    }

    if (showPlayer && hasCurrent) {
        NowPlayingScreen(vm, onBack = { showPlayer = false })
    }

    BackHandler(enabled = showPlayer || currentAlbum != null) {
        when {
            showPlayer -> showPlayer = false
            currentAlbum != null -> vm.clearAlbum()
        }
    }
}
