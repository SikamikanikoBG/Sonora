package com.sikamikaniko.sonora.ui

import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.sikamikaniko.sonora.BuildConfig

/**
 * Ads are enabled ONLY in the official signed build (BuildConfig.ADS_ENABLED, which
 * is tied to the release keystore). Anyone who compiles Sonora themselves gets a
 * build with no keystore -> no ads. Replace the TEST unit ids below to monetise.
 */
object AdsConfig {
    const val BANNER_UNIT = "ca-app-pub-3940256099942544/6300978111" // TEST id
    val enabled: Boolean get() = BuildConfig.ADS_ENABLED
    fun init(context: Context) {
        if (enabled) runCatching { MobileAds.initialize(context) {} }
    }
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (!AdsConfig.enabled) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdsConfig.BANNER_UNIT
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                runCatching { loadAd(AdRequest.Builder().build()) }
            }
        }
    )
}
