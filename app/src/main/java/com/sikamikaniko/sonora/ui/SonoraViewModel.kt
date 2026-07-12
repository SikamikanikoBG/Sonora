package com.sikamikaniko.sonora.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sikamikaniko.sonora.data.Album
import com.sikamikaniko.sonora.data.AlbumWithSongs
import com.sikamikaniko.sonora.data.Prefs
import com.sikamikaniko.sonora.data.SearchResult3
import com.sikamikaniko.sonora.data.Song
import com.sikamikaniko.sonora.data.Subsonic
import com.sikamikaniko.sonora.playback.PlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SonoraViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    // ---- Auth / session state ----
    private val _loggedIn = MutableStateFlow(prefs.isConfigured)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    // ---- Library state ----
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentAlbum = MutableStateFlow<AlbumWithSongs?>(null)
    val currentAlbum: StateFlow<AlbumWithSongs?> = _currentAlbum.asStateFlow()

    private val _searchResult = MutableStateFlow<SearchResult3?>(null)
    val searchResult: StateFlow<SearchResult3?> = _searchResult.asStateFlow()

    // ---- Player state ----
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()

    private val _artist = MutableStateFlow<String?>(null)
    val artist: StateFlow<String?> = _artist.asStateFlow()

    private val _artworkUri = MutableStateFlow<String?>(null)
    val artworkUri: StateFlow<String?> = _artworkUri.asStateFlow()

    private val _hasCurrent = MutableStateFlow(false)
    val hasCurrent: StateFlow<Boolean> = _hasCurrent.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateNowPlaying()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateNowPlaying()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateNowPlaying()
        }
    }

    init {
        Subsonic.loadFrom(prefs)
        connectController()
        if (_loggedIn.value) loadLibrary()
    }

    private fun connectController() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
            updateNowPlaying()
            startPositionPoller()
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun startPositionPoller() {
        viewModelScope.launch {
            while (isActive) {
                controller?.let { c ->
                    _position.value = c.currentPosition.coerceAtLeast(0)
                    val d = c.duration
                    _duration.value = if (d > 0) d else 0
                }
                delay(500)
            }
        }
    }

    private fun updateNowPlaying() {
        val c = controller ?: return
        val md = c.mediaMetadata
        _title.value = md.title?.toString()
        _artist.value = md.artist?.toString()
        _artworkUri.value = md.artworkUri?.toString()
        _hasCurrent.value = c.currentMediaItem != null
        _isPlaying.value = c.isPlaying
        val d = c.duration
        _duration.value = if (d > 0) d else 0
    }

    // ---- Auth actions ----
    fun login(url: String, user: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                Subsonic.configure(url, user, pass)
                val resp = Subsonic.api?.ping()?.response
                if (resp?.status == "ok") {
                    prefs.save(url.trim().trimEnd('/'), user.trim(), pass)
                    _loggedIn.value = true
                    loadLibrary()
                    onResult(true, null)
                } else {
                    onResult(false, resp?.error?.message ?: "Login failed")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Cannot reach server")
            }
        }
    }

    fun logout() {
        controller?.stop()
        controller?.clearMediaItems()
        prefs.clear()
        Subsonic.reset()
        _albums.value = emptyList()
        _currentAlbum.value = null
        _searchResult.value = null
        _hasCurrent.value = false
        _loggedIn.value = false
    }

    // ---- Library actions ----
    fun loadLibrary() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val resp = Subsonic.api?.getAlbumList2(size = 500)?.response
                if (resp?.error != null) {
                    _error.value = resp.error.message
                } else {
                    _albums.value = resp?.albumList2?.album ?: emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _loading.value = false
            }
        }
    }

    fun openAlbum(id: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                val resp = Subsonic.api?.getAlbum(id)?.response
                _currentAlbum.value = resp?.album
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not load album"
            }
        }
    }

    fun clearAlbum() {
        _currentAlbum.value = null
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResult.value = null
            return
        }
        viewModelScope.launch {
            try {
                val resp = Subsonic.api?.search3(query)?.response
                _searchResult.value = resp?.searchResult3
            } catch (e: Exception) {
                _error.value = e.message ?: "Search failed"
            }
        }
    }

    // ---- Playback actions ----
    fun playSongs(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { toMediaItem(it) }
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    private fun toMediaItem(song: Song): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(song.title ?: "Unknown")
            .setArtist(song.artist ?: "Unknown artist")
            .setAlbumTitle(song.album)
            .apply {
                Subsonic.coverArtUrl(song.coverArt, 512)?.let { setArtworkUri(Uri.parse(it)) }
            }
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(Subsonic.streamUrl(song.id))
            .setMediaMetadata(meta)
            .build()
    }

    override fun onCleared() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        super.onCleared()
    }
}
