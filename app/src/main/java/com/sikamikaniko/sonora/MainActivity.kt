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
import com.sikamikaniko.sonora.ui.SonoraRoot
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
            val vm: SonoraViewModel = viewModel()
            val dynamic by vm.dynamicColor.collectAsState()
            SonoraTheme(dynamicColor = dynamic) {
                SonoraRoot(vm)
            }
        }
    }
}
