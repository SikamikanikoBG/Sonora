package com.sikamikaniko.sonora.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sikamikaniko.sonora.BuildConfig
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.sikamikaniko.sonora.data.AiClient
import com.sikamikaniko.sonora.data.Album
import com.sikamikaniko.sonora.data.AlbumWithSongs
import com.sikamikaniko.sonora.data.ArtPalette
import com.sikamikaniko.sonora.data.Artist
import com.sikamikaniko.sonora.data.ArtistWithAlbums
import com.sikamikaniko.sonora.data.Genre
import com.sikamikaniko.sonora.data.LocalMedia
import com.sikamikaniko.sonora.data.OnlineLyrics
import com.sikamikaniko.sonora.data.Playlist
import com.sikamikaniko.sonora.data.PlaylistWithSongs
import com.sikamikaniko.sonora.data.Prefs
import com.sikamikaniko.sonora.data.SearchResult3
import com.sikamikaniko.sonora.data.Song
import com.sikamikaniko.sonora.data.Subsonic
import com.sikamikaniko.sonora.data.Updater
import com.sikamikaniko.sonora.playback.PlaybackService
import com.sikamikaniko.sonora.playback.PlayerCache
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

enum class SelMode { NONE, SONGS, ALBUMS }

class SonoraViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)

    val serverUrl: String? get() = prefs.baseUrl
    val username: String? get() = prefs.username

    // ---- Auth / session ----
    private val _loggedIn = MutableStateFlow(prefs.isConfigured)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    // ---- Library ----
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _genres = MutableStateFlow<List<Genre>>(emptyList())
    val genres: StateFlow<List<Genre>> = _genres.asStateFlow()

    private val _genreAlbums = MutableStateFlow<List<Album>>(emptyList())
    val genreAlbums: StateFlow<List<Album>> = _genreAlbums.asStateFlow()

    private val _albumSort = MutableStateFlow("alphabeticalByName")
    val albumSort: StateFlow<String> = _albumSort.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // ---- Appearance / settings ----
    private val _dynamicColor = MutableStateFlow(prefs.dynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _appTheme = MutableStateFlow(
        runCatching { AppTheme.valueOf(prefs.themeName) }.getOrDefault(AppTheme.MIDNIGHT)
    )
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()
    fun setTheme(t: AppTheme) { prefs.themeName = t.name; _appTheme.value = t }

    // Album-art auto theming
    private val _artTheme = MutableStateFlow(prefs.artTheme)
    val artTheme: StateFlow<Boolean> = _artTheme.asStateFlow()
    private val _artBrush = MutableStateFlow<Brush?>(null)
    val artBrush: StateFlow<Brush?> = _artBrush.asStateFlow()
    private val _albumTitle = MutableStateFlow<String?>(null)
    private var lastArtUrl: String? = null

    fun setArtTheme(v: Boolean) {
        prefs.artTheme = v
        _artTheme.value = v
        if (!v) _artBrush.value = null
        else extractArtColors(_artworkUri.value)
    }

    private fun extractArtColors(url: String?) {
        if (!_artTheme.value) { _artBrush.value = null; return }
        viewModelScope.launch {
            val pair = ArtPalette.colors(getApplication<Application>(), url)
            _artBrush.value = pair?.let {
                Brush.linearGradient(listOf(Color(it.first), Color(it.second)))
            }
        }
    }

    // ---- Local device library ----
    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    fun scanDevice() = viewModelScope.launch {
        _scanning.value = true
        _localSongs.value = try { LocalMedia.scan(getApplication<Application>()) } catch (_: Exception) { emptyList() }
        _scanning.value = false
    }

    // ---- Recent searches ----
    private val _recentSearches = MutableStateFlow(prefs.recentSearches)
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()
    fun addRecentSearch(q: String) {
        val t = q.trim()
        if (t.isBlank()) return
        val list = (listOf(t) + _recentSearches.value.filter { !it.equals(t, true) }).take(12)
        prefs.recentSearches = list
        _recentSearches.value = list
    }
    fun clearRecentSearches() {
        prefs.recentSearches = emptyList()
        _recentSearches.value = emptyList()
    }

    // ---- AI ----
    private val aiGson = Gson()
    private val _aiEnabled = MutableStateFlow(prefs.aiEnabled)
    val aiEnabled: StateFlow<Boolean> = _aiEnabled.asStateFlow()
    private val _aiBaseUrl = MutableStateFlow(prefs.aiBaseUrl)
    val aiBaseUrl: StateFlow<String> = _aiBaseUrl.asStateFlow()
    private val _aiModel = MutableStateFlow(prefs.aiModel)
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()
    private val _aiLang = MutableStateFlow(prefs.aiLang)
    val aiLang: StateFlow<String> = _aiLang.asStateFlow()
    private val _aiModels = MutableStateFlow<List<String>>(emptyList())
    val aiModels: StateFlow<List<String>> = _aiModels.asStateFlow()
    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()
    private val _aiStatus = MutableStateFlow<String?>(null)
    val aiStatus: StateFlow<String?> = _aiStatus.asStateFlow()
    private val _aiText = MutableStateFlow("")
    val aiText: StateFlow<String> = _aiText.asStateFlow()
    private val _aiStreaming = MutableStateFlow(false)
    val aiStreaming: StateFlow<Boolean> = _aiStreaming.asStateFlow()

    val aiReady: Boolean
        get() = _aiEnabled.value && _aiBaseUrl.value.isNotBlank() && _aiModel.value.isNotBlank()

    fun setAiEnabled(v: Boolean) { prefs.aiEnabled = v; _aiEnabled.value = v }
    fun setAiBaseUrl(v: String) { prefs.aiBaseUrl = v; _aiBaseUrl.value = v }
    fun setAiModel(v: String) { prefs.aiModel = v; _aiModel.value = v }
    fun setAiLang(v: String) { prefs.aiLang = v; _aiLang.value = v }
    fun loadAiModels() = viewModelScope.launch {
        _aiModels.value = AiClient.listModels(_aiBaseUrl.value)
    }
    fun clearAiText() { _aiText.value = ""; _aiStreaming.value = false }

    private data class DjResult(val albumIds: List<String>? = null)

    private suspend fun pickAlbums(prompt: String, count: Int): List<Album> {
        val lib = _albums.value
        if (lib.isEmpty()) return emptyList()
        val catalog = lib.joinToString("\n") { a ->
            "${a.id} | ${a.artist ?: "?"} — ${a.name ?: "?"}${a.year?.let { " ($it)" } ?: ""}"
        }
        val sys = "You are an expert music DJ. From the user's library below (format: id | Artist — Album), " +
            "choose about $count albums that best fit the request. Reply ONLY with JSON: {\"albumIds\":[\"id\",...]}. " +
            "Use ONLY ids that appear in the library."
        val user = "Request: $prompt\n\nLibrary:\n$catalog"
        val json = AiClient.chat(
            _aiBaseUrl.value, _aiModel.value,
            listOf(AiClient.Msg("system", sys), AiClient.Msg("user", user)),
            json = true
        ) ?: return emptyList()
        val ids = try { aiGson.fromJson(json, DjResult::class.java)?.albumIds } catch (_: Exception) { null } ?: return emptyList()
        val byId = lib.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    private suspend fun gatherSongs(albums: List<Album>): List<Song> =
        albums.flatMap { Subsonic.api?.getAlbum(it.id)?.response?.album?.song ?: emptyList() }

    /** AI DJ: turn a natural-language request into a playing queue. */
    fun aiDj(prompt: String) = viewModelScope.launch {
        if (!aiReady) { _aiStatus.value = "Set up AI in Settings first"; return@launch }
        if (prompt.isBlank()) return@launch
        _aiBusy.value = true
        _aiStatus.value = "Thinking…"
        try {
            val albums = pickAlbums(prompt, 10)
            if (albums.isEmpty()) { _aiStatus.value = "Couldn't find matches for that"; return@launch }
            _aiStatus.value = "Gathering tracks…"
            val songs = gatherSongs(albums).shuffled()
            if (songs.isEmpty()) { _aiStatus.value = "No playable tracks found"; return@launch }
            _aiStatus.value = "Playing ${songs.size} tracks · ${albums.size} albums"
            playSongs(songs, 0)
        } catch (e: Exception) {
            _aiStatus.value = "AI error — check your endpoint/model"
        } finally {
            _aiBusy.value = false
        }
    }

    // ---- Smart Radio ----
    private val _radio = MutableStateFlow(false)
    val radio: StateFlow<Boolean> = _radio.asStateFlow()
    private var radioSeed: String? = null
    private var radioBusy = false

    fun setRadio(v: Boolean) { _radio.value = v; if (v) maybeExtendRadio() }
    fun toggleRadio() = setRadio(!_radio.value)
    fun startRadioFromCurrent() {
        radioSeed = "${_artist.value ?: ""} — ${_title.value ?: ""}"
        setRadio(true)
    }

    private fun maybeExtendRadio() {
        val c = controller ?: return
        if (!_radio.value || radioBusy || !aiReady) return
        val remaining = c.mediaItemCount - (c.currentMediaItemIndex + 1)
        if (remaining > 3) return
        radioBusy = true
        viewModelScope.launch {
            try {
                val seed = radioSeed ?: "${_artist.value ?: ""} ${_albumTitle.value ?: ""}"
                val albums = pickAlbums("Music similar in style and mood to: $seed. Keep it varied.", 4)
                val songs = gatherSongs(albums).shuffled().take(20)
                if (songs.isNotEmpty()) addToQueue(songs)
            } catch (_: Exception) {
            } finally {
                radioBusy = false
            }
        }
    }

    /** AI insights about the current artist/album (streaming). */
    fun aiInsights() = viewModelScope.launch {
        if (!aiReady) { _aiText.value = "Set up AI in Settings first."; return@launch }
        _aiText.value = ""
        _aiStreaming.value = true
        val sys = "You are a concise, knowledgeable music writer. Write 3-4 sentences. " +
            "If you are unsure of facts, stay general and brief. Plain text, no markdown."
        val user = "Tell me about the artist \"${_artist.value ?: ""}\"" +
            (_albumTitle.value?.let { ", focusing on the album \"$it\"" } ?: "") + "."
        AiClient.chatStream(_aiBaseUrl.value, _aiModel.value, listOf(AiClient.Msg("system", sys), AiClient.Msg("user", user))) { tok ->
            _aiText.value += tok
        }
        _aiStreaming.value = false
    }

    /** AI on lyrics: mode = "translate" or "explain" (streaming). */
    fun aiLyrics(mode: String) = viewModelScope.launch {
        if (!aiReady) { _aiText.value = "Set up AI in Settings first."; return@launch }
        val lyr = _lyrics.value
        if (lyr.isNullOrBlank()) { _aiText.value = "No lyrics to work with yet."; return@launch }
        _aiText.value = ""
        _aiStreaming.value = true
        val prompt = if (mode == "translate")
            "Translate these song lyrics to ${_aiLang.value}. Keep the line breaks. Output only the translation.\n\n$lyr"
        else
            "In a short paragraph, explain the meaning and themes of these song lyrics. Plain text.\n\n$lyr"
        AiClient.chatStream(_aiBaseUrl.value, _aiModel.value, listOf(AiClient.Msg("user", prompt))) { tok ->
            _aiText.value += tok
        }
        _aiStreaming.value = false
    }

    private val _cacheBytes = MutableStateFlow(0L)
    val cacheBytes: StateFlow<Long> = _cacheBytes.asStateFlow()

    // ---- Add-to-playlist picker ----
    private val _playlistPickerSongs = MutableStateFlow<List<Song>?>(null)
    val playlistPickerSongs: StateFlow<List<Song>?> = _playlistPickerSongs.asStateFlow()

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

    // ---- Multi-select ----
    private val _selMode = MutableStateFlow(SelMode.NONE)
    val selMode: StateFlow<SelMode> = _selMode.asStateFlow()
    private val _selSongs = MutableStateFlow<List<Song>>(emptyList())
    val selectedSongs: StateFlow<List<Song>> = _selSongs.asStateFlow()
    private val _selAlbums = MutableStateFlow<List<Album>>(emptyList())
    val selectedAlbums: StateFlow<List<Album>> = _selAlbums.asStateFlow()

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
    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()
    private val _currentAlbumId = MutableStateFlow<String?>(null)
    val currentAlbumId: StateFlow<String?> = _currentAlbumId.asStateFlow()
    private val _currentArtistId = MutableStateFlow<String?>(null)
    val currentArtistId: StateFlow<String?> = _currentArtistId.asStateFlow()

    // ---- Lyrics ----
    private val _lyrics = MutableStateFlow<String?>(null)
    val lyrics: StateFlow<String?> = _lyrics.asStateFlow()
    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    // ---- Sleep timer ----
    private var sleepJob: Job? = null
    private var pauseAfterTrack = false
    private val _sleepMinutesLeft = MutableStateFlow(0)
    val sleepMinutesLeft: StateFlow<Int> = _sleepMinutesLeft.asStateFlow()
    private val _sleepEndOfTrack = MutableStateFlow(false)
    val sleepEndOfTrack: StateFlow<Boolean> = _sleepEndOfTrack.asStateFlow()

    // ---- Self-update ----
    private val _update = MutableStateFlow<Updater.UpdateInfo?>(null)
    val update: StateFlow<Updater.UpdateInfo?> = _update.asStateFlow()
    private val _updateBusy = MutableStateFlow(false)
    val updateBusy: StateFlow<Boolean> = _updateBusy.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { updateNowPlaying() }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (pauseAfterTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                controller?.pause()
                pauseAfterTrack = false
                _sleepEndOfTrack.value = false
            }
            updateNowPlaying()
            rebuildQueue()
            mediaItem?.mediaId?.let { scrobble(it) }
            if (_radio.value) maybeExtendRadio()
        }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            rebuildQueue()
        }
        override fun onPlaybackStateChanged(playbackState: Int) { updateNowPlaying() }
        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
            _speed.value = playbackParameters.speed
        }
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
        viewModelScope.launch { _update.value = Updater.check(BuildConfig.VERSION_NAME) }
    }
    fun dismissUpdate() { _update.value = null }
    fun downloadAndInstallUpdate() {
        val info = _update.value ?: return
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            _updateBusy.value = true
            val file = Updater.download(ctx, info.apkUrl)
            _updateBusy.value = false
            if (file != null) Updater.install(ctx, file) else _error.value = "Update download failed"
        }
    }

    fun consumeToast() { _toast.value = null }

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
        _albumTitle.value = md.albumTitle?.toString()
        val art = md.artworkUri?.toString()
        _artworkUri.value = art
        if (art != lastArtUrl) {
            lastArtUrl = art
            extractArtColors(art)
        }
        _currentMediaId.value = c.currentMediaItem?.mediaId
        _hasCurrent.value = c.currentMediaItem != null
        _isPlaying.value = c.isPlaying
        _repeatMode.value = c.repeatMode
        _shuffle.value = c.shuffleModeEnabled
        _speed.value = c.playbackParameters.speed
        val extras = md.extras
        _currentAlbumId.value = extras?.getString("albumId")
        _currentArtistId.value = extras?.getString("artistId")
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
                } else onResult(false, resp?.error?.message ?: "Login failed")
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
        clearSelection()
        _hasCurrent.value = false
        _loggedIn.value = false
    }

    fun refreshAll() {
        loadLibrary(); loadHome(); loadArtists(); loadGenres(); loadPlaylists(); loadStarred()
        refreshCacheSize()
    }

    // ---- Loaders ----
    fun loadLibrary() = viewModelScope.launch {
        _loading.value = true; _error.value = null
        try {
            val resp = Subsonic.api?.getAlbumList2(_albumSort.value, 500)?.response
            if (resp?.error != null) _error.value = resp.error.message
            else _albums.value = resp?.albumList2?.album ?: emptyList()
        } catch (e: Exception) { _error.value = e.message ?: "Network error" }
        finally { _loading.value = false }
    }

    fun setAlbumSort(type: String) {
        if (_albumSort.value == type) return
        _albumSort.value = type
        loadLibrary()
    }

    fun loadGenres() = viewModelScope.launch {
        try { _genres.value = Subsonic.api?.getGenres()?.response?.genres?.genre ?: emptyList() }
        catch (_: Exception) { }
    }

    fun loadGenreAlbums(name: String) = viewModelScope.launch {
        try {
            _genreAlbums.value = Subsonic.api?.getAlbumList2("byGenre", 500, 0, name)?.response?.albumList2?.album ?: emptyList()
        } catch (_: Exception) { _genreAlbums.value = emptyList() }
    }

    fun shuffleLibrary() = viewModelScope.launch {
        val songs = Subsonic.api?.getRandomSongs(150)?.response?.randomSongs?.song ?: emptyList()
        shufflePlay(songs)
    }

    // ---- Appearance / cache ----
    fun setDynamicColor(v: Boolean) { prefs.dynamicColor = v; _dynamicColor.value = v }
    fun refreshCacheSize() { _cacheBytes.value = PlayerCache.sizeBytes() }
    fun clearCache() { PlayerCache.clear(); refreshCacheSize(); _toast.value = "Cache cleared" }

    // ---- Add to playlist ----
    fun openPlaylistPicker(songs: List<Song>) { if (songs.isNotEmpty()) _playlistPickerSongs.value = songs }
    fun dismissPlaylistPicker() { _playlistPickerSongs.value = null }
    fun openPlaylistPickerFromSelection() = viewModelScope.launch {
        val songs = resolveSelection()
        clearSelection()
        if (songs.isNotEmpty()) _playlistPickerSongs.value = songs
    }
    fun addPickerSongsToPlaylist(playlistId: String) {
        val songs = _playlistPickerSongs.value ?: return
        _playlistPickerSongs.value = null
        viewModelScope.launch {
            try {
                Subsonic.api?.updatePlaylist(playlistId, songs.map { it.id })
                loadPlaylists()
                _toast.value = "Added ${songs.size} to playlist"
            } catch (_: Exception) { _error.value = "Could not add to playlist" }
        }
    }
    fun createPlaylistWithPickerSongs(name: String) {
        val songs = _playlistPickerSongs.value ?: return
        _playlistPickerSongs.value = null
        viewModelScope.launch {
            try {
                Subsonic.api?.createPlaylist(name.trim(), songs.map { it.id })
                loadPlaylists()
                _toast.value = "Created \"$name\""
            } catch (_: Exception) { _error.value = "Could not create playlist" }
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
        try { _currentAlbum.value = Subsonic.api?.getAlbum(id)?.response?.album }
        catch (e: Exception) { _error.value = e.message ?: "Could not load album" }
    }

    fun openArtist(id: String) = viewModelScope.launch {
        _error.value = null
        try { _currentArtist.value = Subsonic.api?.getArtist(id)?.response?.artist }
        catch (e: Exception) { _error.value = e.message ?: "Could not load artist" }
    }

    fun openPlaylist(id: String) = viewModelScope.launch {
        _error.value = null
        try { _currentPlaylist.value = Subsonic.api?.getPlaylist(id)?.response?.playlist }
        catch (e: Exception) { _error.value = e.message ?: "Could not load playlist" }
    }

    fun search(query: String) {
        if (query.isBlank()) { _searchResult.value = null; return }
        viewModelScope.launch {
            try { _searchResult.value = Subsonic.api?.search3(query)?.response?.searchResult3 }
            catch (e: Exception) { _error.value = e.message ?: "Search failed" }
        }
    }

    // ---- Favourites ----
    fun toggleStar(id: String) {
        val currentlyStarred = _starredIds.value.contains(id)
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

    // ---- Selection ----
    fun toggleSongSelection(song: Song) {
        if (_selMode.value == SelMode.ALBUMS) clearSelection()
        val cur = _selSongs.value
        val next = if (cur.any { it.id == song.id }) cur.filter { it.id != song.id } else cur + song
        _selSongs.value = next
        _selMode.value = if (next.isEmpty()) SelMode.NONE else SelMode.SONGS
    }

    fun toggleAlbumSelection(album: Album) {
        if (_selMode.value == SelMode.SONGS) clearSelection()
        val cur = _selAlbums.value
        val next = if (cur.any { it.id == album.id }) cur.filter { it.id != album.id } else cur + album
        _selAlbums.value = next
        _selMode.value = if (next.isEmpty()) SelMode.NONE else SelMode.ALBUMS
    }

    fun isSongSelected(id: String) = _selSongs.value.any { it.id == id }
    fun isAlbumSelected(id: String) = _selAlbums.value.any { it.id == id }

    fun clearSelection() {
        _selMode.value = SelMode.NONE
        _selSongs.value = emptyList()
        _selAlbums.value = emptyList()
    }

    private suspend fun resolveSelection(): List<Song> = when (_selMode.value) {
        SelMode.SONGS -> _selSongs.value
        SelMode.ALBUMS -> _selAlbums.value.flatMap { al ->
            Subsonic.api?.getAlbum(al.id)?.response?.album?.song ?: emptyList()
        }
        SelMode.NONE -> emptyList()
    }

    fun playSelection(shuffle: Boolean) = viewModelScope.launch {
        val songs = resolveSelection()
        clearSelection()
        if (shuffle) shufflePlay(songs) else playSongs(songs, 0)
    }

    fun queueSelection() = viewModelScope.launch {
        val songs = resolveSelection()
        val n = songs.size
        clearSelection()
        addToQueue(songs)
        _toast.value = "Added $n to queue"
    }

    fun playNextSelection() = viewModelScope.launch {
        val songs = resolveSelection()
        clearSelection()
        playNext(songs)
    }

    // ---- Bulk "play all" ----
    private suspend fun songsOfArtist(artistId: String): List<Song> {
        val albums = Subsonic.api?.getArtist(artistId)?.response?.artist?.album ?: emptyList()
        return albums.flatMap { Subsonic.api?.getAlbum(it.id)?.response?.album?.song ?: emptyList() }
    }

    fun playArtist(artistId: String, shuffle: Boolean) = viewModelScope.launch {
        val songs = songsOfArtist(artistId)
        if (shuffle) shufflePlay(songs) else playSongs(songs, 0)
    }

    fun playSearchSongs(shuffle: Boolean) {
        val songs = _searchResult.value?.song ?: return
        if (shuffle) shufflePlay(songs) else playSongs(songs, 0)
    }

    // ---- Playback ----
    fun playSongs(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { toMediaItem(it) }
        c.shuffleModeEnabled = false
        c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        c.prepare(); c.play()
    }

    fun shufflePlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        val c = controller ?: return
        val items = songs.map { toMediaItem(it) }
        c.setMediaItems(items, 0, 0L)
        c.shuffleModeEnabled = true
        c.prepare(); c.play()
    }

    fun addToQueue(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { toMediaItem(it) }
        if (c.mediaItemCount == 0) { c.setMediaItems(items); c.prepare(); c.play() }
        else c.addMediaItems(items)
        rebuildQueue()
    }

    fun playNext(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { toMediaItem(it) }
        if (c.mediaItemCount == 0) { c.setMediaItems(items); c.prepare(); c.play() }
        else c.addMediaItems((c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount), items)
        rebuildQueue()
    }

    fun removeFromQueue(index: Int) {
        val c = controller ?: return
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
        rebuildQueue()
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
                delay(60_000); left--; _sleepMinutesLeft.value = left
            }
            if (isActive) { controller?.pause(); _sleepMinutesLeft.value = 0 }
        }
    }

    fun sleepAtEndOfTrack() {
        sleepJob?.cancel(); sleepJob = null; _sleepMinutesLeft.value = 0
        pauseAfterTrack = true
        _sleepEndOfTrack.value = true
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel(); sleepJob = null; _sleepMinutesLeft.value = 0
        pauseAfterTrack = false
        _sleepEndOfTrack.value = false
    }

    // ---- Playback speed ----
    fun setSpeed(value: Float) {
        controller?.setPlaybackSpeed(value)
        _speed.value = value
    }

    // ---- Queue reorder ----
    fun moveQueueUp(index: Int) {
        val c = controller ?: return
        if (index > 0) { c.moveMediaItem(index, index - 1); rebuildQueue() }
    }
    fun moveQueueDown(index: Int) {
        val c = controller ?: return
        if (index < c.mediaItemCount - 1) { c.moveMediaItem(index, index + 1); rebuildQueue() }
    }
    fun moveQueueBy(from: Int, steps: Int) {
        val c = controller ?: return
        if (steps == 0) return
        val to = (from + steps).coerceIn(0, c.mediaItemCount - 1)
        if (to != from) { c.moveMediaItem(from, to); rebuildQueue() }
    }

    // ---- Lyrics ----
    fun loadLyrics() = viewModelScope.launch {
        _lyricsLoading.value = true
        // 1) try the music server
        var text = try {
            Subsonic.api?.getLyrics(_artist.value, _title.value)?.response?.lyrics?.value
        } catch (_: Exception) { null }
        // 2) fall back to lrclib.net (most self-hosted libraries have no lyrics)
        if (text.isNullOrBlank()) {
            text = OnlineLyrics.fetch(
                _artist.value, _title.value, _albumTitle.value, (_duration.value / 1000).toInt()
            )
        }
        _lyrics.value = text
        _lyricsLoading.value = false
    }

    // ---- Playlist management ----
    fun deletePlaylist(id: String) = viewModelScope.launch {
        try {
            Subsonic.api?.deletePlaylist(id)
            _currentPlaylist.value = null
            loadPlaylists()
            _toast.value = "Playlist deleted"
        } catch (_: Exception) { _error.value = "Could not delete playlist" }
    }

    fun removeFromPlaylist(playlistId: String, index: Int) = viewModelScope.launch {
        try {
            Subsonic.api?.updatePlaylist(playlistId, null, listOf(index))
            _currentPlaylist.value = Subsonic.api?.getPlaylist(playlistId)?.response?.playlist
        } catch (_: Exception) { _error.value = "Could not remove song" }
    }

    private fun scrobble(id: String) {
        if (id.startsWith("local:")) return
        viewModelScope.launch { try { Subsonic.api?.scrobble(id) } catch (_: Exception) { } }
    }

    fun clearAlbum() { _currentAlbum.value = null }
    fun clearArtist() { _currentArtist.value = null }
    fun clearPlaylist() { _currentPlaylist.value = null }

    private fun toMediaItem(song: Song): MediaItem {
        val isLocal = song.localUri != null
        val extras = Bundle().apply {
            if (!isLocal) {
                song.albumId?.let { putString("albumId", it) }
                song.artistId?.let { putString("artistId", it) }
            }
        }
        val artUrl = song.artUri ?: Subsonic.coverArtUrl(song.coverArt, 512)
        val meta = MediaMetadata.Builder()
            .setTitle(song.title ?: "Unknown")
            .setArtist(song.artist ?: "Unknown artist")
            .setAlbumTitle(song.album)
            .setExtras(extras)
            .apply { artUrl?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.localUri ?: Subsonic.streamUrl(song.id))
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
