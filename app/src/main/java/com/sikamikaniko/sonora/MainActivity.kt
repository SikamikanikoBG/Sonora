package com.sikamikaniko.sonora

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sikamikaniko.sonora.data.Prefs
import com.sikamikaniko.sonora.ui.SonoraRoot
import com.sikamikaniko.sonora.ui.SonoraTheme
import com.sikamikaniko.sonora.ui.SonoraViewModel

class MainActivity : ComponentActivity() {

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asked together: two separate launches in onCreate race and one gets dropped.
        val wanted = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Android 12+ withholds the Bluetooth-connected broadcast without this, so
            // "start when my car connects" silently never fires.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Prefs(this@MainActivity).btAutoplay) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        if (wanted.isNotEmpty()) requestPermissions.launch(wanted.toTypedArray())
        setContent {
            val vm: SonoraViewModel = viewModel()
            val theme by vm.appTheme.collectAsState()
            SonoraTheme(theme) {
                SonoraRoot(vm)
            }
        }
    }
}
