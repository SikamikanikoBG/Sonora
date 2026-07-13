package com.sikamikaniko.sonora

import android.app.Application
import com.sikamikaniko.sonora.data.Prefs
import com.sikamikaniko.sonora.data.Subsonic
import com.sikamikaniko.sonora.ui.AdsConfig

class SonoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Restore an existing server connection, if the user already logged in.
        Subsonic.loadFrom(Prefs(this))
        // Initialise ads (no-op unless this is the official signed build).
        AdsConfig.init(this)
    }
}
