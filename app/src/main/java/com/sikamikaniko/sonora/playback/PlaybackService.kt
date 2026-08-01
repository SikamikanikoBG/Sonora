package com.sikamikaniko.sonora.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sikamikaniko.sonora.MainActivity

/**
 * Foreground media service. Media3 handles the notification and lock-screen
 * controls automatically once a [MediaSession] wraps the [ExoPlayer].
 * Streams go through a caching data source so playback is smooth and cached.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var guard: StreamGuard? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Buffer deeply so a coverage gap is ridden out from RAM and disk instead of
        // stopping playback. Five minutes of audio comfortably covers a tunnel or a
        // valley; `prioritizeTimeOverSizeThresholds` keeps filling toward the minimum
        // even on a big file, and the byte cap keeps that honest on low-RAM phones.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 60_000,
                /* maxBufferMs = */ 300_000,
                /* bufferForPlaybackMs = */ 2_000,
                /* bufferForPlaybackAfterRebufferMs = */ 4_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(32 * 1024 * 1024)
            // Keep a minute behind us so re-preparing after a dropout resumes from
            // memory rather than re-fetching what we already played.
            .setBackBuffer(/* backBufferDurationMs = */ 60_000, /* retainBackBufferFromKeyframe = */ true)
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(Streaming.cacheFactory(this))
            .setLoadErrorHandlingPolicy(Streaming.loadErrorPolicy())

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            // Screen off in a phone mount is the normal case in the car: hold the CPU
            // and wifi locks while playing, or the radio sleeps and the stream stalls.
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        // Tapping the media notification / lock-screen controls opens the app.
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        // Reconnect/stall recovery and preload live here, not in the ViewModel: playback
        // routinely outlives the UI (Bluetooth autoplay in the car starts this service
        // with no Activity at all), and that is exactly when it needs protecting.
        guard = StreamGuard(this, player).also { it.start() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        guard?.release()
        guard = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
