package com.sikamikaniko.sonora.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

/**
 * Fetches lyrics from lrclib.net (free, no auth) as a fallback when the music
 * server has none. Tries an exact match first, then a fuzzy search.
 */
object OnlineLyrics {

    private val client = OkHttpClient()
    private val gson = Gson()
    private const val UA = "Sonora/1.1 (https://github.com/SikamikanikoBG/Sonora)"

    data class Lrc(
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
        val instrumental: Boolean? = null
    )

    suspend fun fetch(artist: String?, title: String?, album: String?, durationSec: Int): String? =
        withContext(Dispatchers.IO) {
            if (artist.isNullOrBlank() || title.isNullOrBlank()) return@withContext null
            try {
                // 1) exact get
                val getUrl = buildString {
                    append("https://lrclib.net/api/get?track_name=").append(enc(title))
                    append("&artist_name=").append(enc(artist))
                    if (!album.isNullOrBlank()) append("&album_name=").append(enc(album))
                    if (durationSec > 0) append("&duration=").append(durationSec)
                }
                requestString(getUrl)?.let { body ->
                    val lrc = gson.fromJson(body, Lrc::class.java)
                    plainFrom(lrc)?.let { return@withContext it }
                }
                // 2) fuzzy search -> first usable result
                val searchUrl = "https://lrclib.net/api/search?track_name=${enc(title)}&artist_name=${enc(artist)}"
                requestString(searchUrl)?.let { body ->
                    val arr = JsonParser.parseString(body).asJsonArray
                    for (el in arr) {
                        val lrc = gson.fromJson(el, Lrc::class.java)
                        plainFrom(lrc)?.let { return@withContext it }
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }

    private fun plainFrom(lrc: Lrc?): String? {
        if (lrc == null) return null
        val plain = lrc.plainLyrics
        if (!plain.isNullOrBlank()) return plain
        val synced = lrc.syncedLyrics
        if (!synced.isNullOrBlank()) return stripTimestamps(synced)
        return null
    }

    /** Turns "[00:12.34] line" synced lyrics into plain text. */
    private fun stripTimestamps(synced: String): String =
        synced.lineSequence()
            .map { it.replace(Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?]"), "").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")

    private fun requestString(url: String): String? {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}
