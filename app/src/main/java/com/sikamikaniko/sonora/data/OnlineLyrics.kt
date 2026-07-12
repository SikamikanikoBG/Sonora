package com.sikamikaniko.sonora.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Fetches lyrics from lrclib.net (free, no auth) as a fallback when the music
 * server has none. Tries several strategies, most-precise first, so odd tags,
 * missing albums or slightly-off durations still resolve.
 */
object OnlineLyrics {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private const val UA = "Sonora/1.5 (https://github.com/SikamikanikoBG/Sonora)"

    data class Lrc(
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
        val instrumental: Boolean? = null
    )

    suspend fun fetch(artist: String?, title: String?, album: String?, durationSec: Int): String? =
        withContext(Dispatchers.IO) {
            if (artist.isNullOrBlank() || title.isNullOrBlank()) return@withContext null
            val a = artist.substringBefore(" feat", ignoreCase = true).trim()
            // 1) exact get on title+artist (most reliable single hit)
            getExact(title, a, null, 0)?.let { return@withContext it }
            // 2) exact get including album + duration (if we have them)
            if (!album.isNullOrBlank() || durationSec > 0) {
                getExact(title, a, album, durationSec)?.let { return@withContext it }
            }
            // 3) structured search
            search("track_name=${enc(title)}&artist_name=${enc(a)}")?.let { return@withContext it }
            // 4) free-text search — last resort, most forgiving
            search("q=${enc("$title $a")}")?.let { return@withContext it }
            null
        }

    private fun getExact(title: String, artist: String, album: String?, durationSec: Int): String? {
        val url = buildString {
            append("https://lrclib.net/api/get?track_name=").append(enc(title))
            append("&artist_name=").append(enc(artist))
            if (!album.isNullOrBlank()) append("&album_name=").append(enc(album))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        val body = request(url) ?: return null
        return try { plainFrom(gson.fromJson(body, Lrc::class.java)) } catch (_: Exception) { null }
    }

    private fun search(queryParams: String): String? {
        val body = request("https://lrclib.net/api/search?$queryParams") ?: return null
        return try {
            val arr = JsonParser.parseString(body).asJsonArray
            for (el in arr) {
                plainFrom(gson.fromJson(el, Lrc::class.java))?.let { return it }
            }
            null
        } catch (_: Exception) { null }
    }

    private fun plainFrom(lrc: Lrc?): String? {
        if (lrc == null || lrc.instrumental == true) return null
        lrc.plainLyrics?.takeIf { it.isNotBlank() }?.let { return it }
        lrc.syncedLyrics?.takeIf { it.isNotBlank() }?.let { return stripTimestamps(it) }
        return null
    }

    private fun stripTimestamps(synced: String): String =
        synced.lineSequence()
            .map { it.replace(Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?]"), "").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    private fun request(url: String): String? = try {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) resp.body?.string() else null
        }
    } catch (_: Exception) { null }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
