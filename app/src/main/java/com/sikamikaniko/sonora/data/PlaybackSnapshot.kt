package com.sikamikaniko.sonora.data

/** A minimal, restorable description of one queued item. */
data class SavedTrack(
    val mediaId: String,
    val uri: String,
    val title: String? = null,
    val artist: String? = null,
    val artUri: String? = null
)

/** Snapshot of the whole playback state, persisted so the app can resume exactly. */
data class PlaybackSnapshot(
    val tracks: List<SavedTrack> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0,
    val wasPlaying: Boolean = false
)
