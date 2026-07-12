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
import com.sikamikaniko.sonora.data.Artist
import com.sikamikaniko.sonora.data.ArtistWithAlbums
import com.sikamikaniko.sonora.data.Playlist
import com.sikamikaniko.sonora.data.PlaylistWithSongs
import com.sikamikaniko.sonora.data.Prefs
import com.sikamikaniko.sonora.data.SearchResult3
import com.sikamikaniko.sonora.data.Song
import com.sikamikaniko.sonora.BuildConfig
import com.sikamikaniko.sonora.data.Subsonic
import com.sikamikaniko.sonora.data.Updater
import com.sikamikaniko.sonora.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QueueItem(
    val index: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val isCurrent: Boolean
)

class SonoraViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    // ---- Auth / session ----
    private val _loggedIn = MutableStateFlow(prefs.isConfigured)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ---- Library ----
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // ---- Home rows ----
    private val _newest = MutableStateFlow<List<Album>>(emptyList())
    val newest: StateFlow<List<Album>> = _newest.asStateFlow()
    private val _recent = MutableStateFlow<List<Album>>(emptyList())
    val recent: StateFlow<List<Album>> = _recent.asStateFlow()
    private val _frequent = MutableStateFlow<List<Album>>(emptyList())
    val frequent: StateFlow<List<Album>> = _frequent.asStateFlow()
    private val _randomAlbums = MutableStateFlow<List<Album>>(emptyList())
    val randomAlbums: StateFlow<List<Album>> = _randomAlbums.asStateFlow()

    // ---- Detail ----
    private val _currentAlbum = MutableStateFlow<AlbumWithSongs?>(null)
    val currentAlbum: StateFlow<AlbumWithSongs?> = _currentAlbum.asStateFlow()

    private val _currentArtist = MutableStateFlow<ArtistWithAlbums?>(null)
    val currentArtist: StateFlow<ArtistWithAlbums?> = _currentArtist.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<PlaylistWithSongs?>(null)
    val currentPlaylist: StateFlow<PlaylistWithSongs?> = _currentPlaylist.asStateFlow()

    // ---- Search ----
    private val _searchResult = MutableStateFlow<SearchResult3?>(null)
    val searchResult: StateFlow<SearchResult3?> = _searchResult.asStateFlow()

    // ---- Favourites ----
    private val _starredIds = MutableStateFlow<Set<String>>(emptySet())
    val starredIds: StateFlow<Set<String>> = _starredIds.asStateFlow()
    private val _starredSongs = MutableStateFlow<List<Song>>(emptyList())
    val starredSongs: StateFlow<List<Song>> = _starredSongs.asStateFlow()
    private val _starredAlbums = MutableStateFlow<List<Album>>(emptyList())
    val starredAlbums: StateFlow<List<Album>> = _starredAlbums.asStateFlow()

    // ---- Player ----
    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _title = MutableStateFlow<String?>(null)
    val title: StateFlow<String?> = _title.asStateFlow()
    private val _artist = MutableStateFlow<String?>(null)
    val artist: StateFlow<String?> = _artist.asStateFlow()
    private val _artworkUri = MutableStateFlow<String?>(null)
    val artworkUri: StateFlow<String?> = _artworkUri.asStateFlow()
    private val _currentMediaId = MutableStateFlow<String?>(null)
    val currentMediaId: StateFlow<String?> = _currentMediaId.asStateFlow()
    private val _hasCurrent = MutableStateFlow(false)
    val hasCurrent: StateFlow<Boolean> = _hasCurrent.asStateFlow()
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()

    // ---- Sleep timer ----
    private var sleepJob: Job? = null
    private val _sleepMinutesLeft = MutableStateFlow(0)
    val sleepMinutesLeft: StateFlow<Int> = _sleepMinutesLeft.asStateFlow()

    // ---- Self-update ----
    private val _update = MutableStateFlow<Updater.UpdateInfo?>(null)
    val update: StateFlow<Updater.UpdateInfo?> = _update.asStateFlow()
    private val _updateBusy = MutableStateFlow(false)
    val updateBusy: StateFlow<Boolean> = _updateBusy.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { updateNowPlaying() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateNowPlaying()
            rebuildQueue()
            mediaItem?.mediaId?.let { scrobble(it) }
        }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            rebuildQueue()
        }
        override fun onPlaybackStateChanged(playbackState: Int) { updateNowPlaying() }
        override fun onRepeatModeChanged(repeatMode: Int) { _repeatMode.value = repeatMode }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffle.value = shuffleModeEnabled
        }
    }

    init {
        Subsonic.loadFrom(prefs)
        connectController()
        if (_loggedIn.value) refreshAll()
        checkForUpdate()
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _update.value = Updater.check(BuildConfig.VERSION_NAME)
        }
    }

    fun dismissUpdate() { _update.value = null }

    fun downloadAndInstallUpdate() {
        val info = _update.value ?: return
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _updateBusy.value = true
            val file = Updater.download(ctx, info.apkUrl)
            _updateBusy.value = false
            if (file != null) Updater.install(ctx, file)
            else _error.value = "Update download failed"
        }
    }

    private fun connectController() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(playerListener)
            updateNowPlaying()
            rebuildQueue()
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
        _currentMediaId.value = c.currentMediaItem?.mediaId
        _hasCurrent.value = c.currentMediaItem != null
        _isPlaying.value = c.isPlaying
        _repeatMode.value = c.repeatMode
        _shuffle.value = c.shuffleModeEnabled
        val d = c.duration
        _duration.value = if (d > 0) d else 0
    }

    private fun rebuildQueue() {
        val c = controller ?: return
        val count = c.mediaItemCount
        val list = ArrayList<QueueItem>(count)
        for (i in 0 until count) {
            val mi = c.getMediaItemAt(i)
            list.add(
                QueueItem(
                    index = i,
                    mediaId = mi.mediaId,
                    title = mi.mediaMetadata.title?.toString() ?: "Unknown",
                    artist = mi.mediaMetadata.artist?.toString() ?: "",
                    artworkUri = mi.mediaMetadata.artworkUri?.toString(),
                    isCurrent = i == c.currentMediaItemIndex
                )
            )
        }
        _queue.value = list
    }

    // ---- Auth ----
    fun login(url: String, user: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                Subsonic.configure(url, user, pass)
                val resp = Subsonic.api?.ping()?.response
                if (resp?.status == "ok") {
                    prefs.save(url.trim().trimEnd('/'), user.trim(), pass)
                    _loggedIn.value = true
                    refreshAll()
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
        _albums.value = emptyList(); _artists.value = emptyList()
        _newest.value = emptyList(); _recent.value = emptyList()
        _frequent.value = emptyList(); _randomAlbums.value = emptyList()
        _playlists.value = emptyList(); _currentAlbum.value = null
        _currentArtist.value = null; _currentPlaylist.value = null
        _searchResult.value = null; _starredIds.value = emptySet()
        _starredSongs.value = emptyList(); _starredAlbums.value = emptyList()
        _hasCurrent.value = false
        _loggedIn.value = false
    }

    fun refreshAll() {
        loadLibrary()
        loadHome()
        loadArtists()
        loadPlaylists()
        loadStarred()
    }

    // ---- Loaders ----
    fun loadLibrary() = viewModelScope.launch {
        _loading.value = true
        _error.value = null
        try {
            val resp = Subsonic.api?.getAlbumList2("alphabeticalByName", 500)?.response
            if (resp?.error != null) _error.value = resp.error.message
            else _albums.value = resp?.albumList2?.album ?: emptyList()
        } catch (e: Exception) {
            _error.value = e.message ?: "Network error"
        } finally {
            _loading.value = false
        }
    }

    fun loadHome() = viewModelScope.launch {
        try {
            _newest.value = Subsonic.api?.getAlbumList2("newest", 20)?.response?.albumList2?.album ?: emptyList()
            _recent.value = Subsonic.api?.getAlbumList2("recent", 20)?.response?.albumList2?.album ?: emptyList()
            _frequent.value = Subsonic.api?.getAlbumList2("frequent", 20)?.response?.albumList2?.album ?: emptyList()
            _randomAlbums.value = Subsonic.api?.getAlbumList2("random", 20)?.response?.albumList2?.album ?: emptyList()
        } catch (_: Exception) { }
    }

    fun loadArtists() = viewModelScope.launch {
        try {
            val indexes = Subsonic.api?.getArtists()?.response?.artists?.index ?: emptyList()
            _artists.value = indexes.flatMap { it.artist ?: emptyList() }
        } catch (_: Exception) { }
    }

    fun loadPlaylists() = viewModelScope.launch {
        try {
            _playlists.value = Subsonic.api?.getPlaylists()?.response?.playlists?.playlist ?: emptyList()
        } catch (_: Exception) { }
    }

    fun loadStarred() = viewModelScope.launch {
        try {
            val s = Subsonic.api?.getStarred2()?.response?.starred2
            val songs = s?.song ?: emptyList()
            _starredSongs.value = songs
            _starredAlbums.value = s?.album ?: emptyList()
            val ids = HashSet<String>()
            songs.forEach { ids.add(it.id) }
            s?.album?.forEach { ids.add(it.id) }
            s?.artist?.forEach { ids.add(it.id) }
            _starredIds.value = ids
        } catch (_: Exception) { }
    }

    fun openAlbum(id: String) = viewModelScope.launch {
        _error.value = null
        try {
            _currentAlbum.value = Subsonic.api?.getAlbum(id)?.response?.album
        } catch (e: Exception) { _error.value = e.message ?: "Could not load album" }
    }

    fun openArtist(id: String) = viewModelScope.launch {
        _error.value = null
        try {
            _currentArtist.value = Subsonic.api?.getArtist(id)?.response?.artist
        } catch (e: Exception) { _error.value = e.message ?: "Could not load artist" }
    }

    fun openPlaylist(id: String) = viewModelScope.launch {
        _error.value = null
        try {
            _currentPlaylist.value = Subsonic.api?.getPlaylist(id)?.response?.playlist
        } catch (e: Exception) { _error.value = e.message ?: "Could not load playlist" }
    }

    fun search(query: String) {
        if (query.isBlank()) { _searchResult.value = null; return }
        viewModelScope.launch {
            try {
                _searchResult.value = Subsonic.api?.search3(query)?.response?.searchResult3
            } catch (e: Exception) { _error.value = e.message ?: "Search failed" }
        }
    }

    // ---- Favourites ----
    fun toggleStar(id: String) {
        val currentlyStarred = _starredIds.value.contains(id)
        // optimistic update
        _starredIds.value = _starredIds.value.toMutableSet().apply {
            if (currentlyStarred) remove(id) else add(id)
        }
        viewModelScope.launch {
            try {
                if (currentlyStarred) Subsonic.api?.unstar(id) else Subsonic.api?.star(id)
                loadStarred()
            } catch (_: Exception) { }
        }
    }

    fun isStarred(id: String?): Boolean = id != null && _starredIds.value.contains(id)

    // ---- Playback ----
    fun playSongs(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { toMediaItem(it) }
        c.shuffleModeEnabled = false
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun shufflePlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val items = songs.map { toMediaItem(it) }
        c.setMediaItems(items, 0, 0L)
        c.shuffleModeEnabled = true
        c.prepare()
        c.play()
    }

    fun togglePlay() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun playFromQueue(index: Int) { controller?.let { it.seekTo(index, 0L); it.play() } }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = c.repeatMode
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
        _shuffle.value = c.shuffleModeEnabled
    }

    // ---- Sleep timer ----
    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        _sleepMinutesLeft.value = minutes
        sleepJob = viewModelScope.launch {
            var left = minutes
            while (left > 0 && isActive) {
                delay(60_000)
                left--
                _sleepMinutesLeft.value = left
            }
            if (isActive) {
                controller?.pause()
                _sleepMinutesLeft.value = 0
            }
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepMinutesLeft.value = 0
    }

    private fun scrobble(id: String) {
        viewModelScope.launch { try { Subsonic.api?.scrobble(id) } catch (_: Exception) { } }
    }

    fun clearAlbum() { _currentAlbum.value = null }
    fun clearArtist() { _currentArtist.value = null }
    fun clearPlaylist() { _currentPlaylist.value = null }

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
