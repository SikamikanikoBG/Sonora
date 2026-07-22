package com.sikamikaniko.sonora.data

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Free, keyless Wikipedia lookup used to GROUND the AI (retrieval-augmented) so it
 * states real facts instead of hallucinating. Returns a plain-text intro extract.
 */
object Wikipedia {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
    private const val UA = "Sonora/1.6 (https://github.com/SikamikanikoBG/Sonora)"

    /** A matched article: intro text plus the lead photo and canonical link. */
    data class Page(
        val title: String?,
        val extract: String,
        val imageUrl: String? = null,
        val pageUrl: String? = null
    )

    /** Best-matching article intro (plain text, ~1500 chars) for the query, or null. */
    suspend fun lookup(query: String): String? = lookupPage(query)?.extract

    /** Full lookup: intro extract + lead image + page URL, or null when nothing matches. */
    suspend fun lookupPage(query: String): Page? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        try {
            val url = "https://en.wikipedia.org/w/api.php?action=query&format=json&generator=search" +
                "&gsrsearch=${enc(query)}&gsrlimit=1&redirects=1" +
                "&prop=extracts%7Cpageimages%7Cinfo&exintro=1&explaintext=1&exchars=1600" +
                "&piprop=thumbnail&pithumbsize=800&inprop=url"
            val body = request(url) ?: return@withContext null
            val pages = JsonParser.parseString(body).asJsonObject
                .getAsJsonObject("query")?.getAsJsonObject("pages") ?: return@withContext null
            val entry = pages.entrySet().firstOrNull()?.value?.asJsonObject ?: return@withContext null
            val extract = entry.get("extract")?.asString?.trim()?.takeIf { it.isNotBlank() }
                ?: return@withContext null
            Page(
                title = entry.get("title")?.asString,
                extract = extract,
                imageUrl = entry.getAsJsonObject("thumbnail")?.get("source")?.asString,
                pageUrl = entry.get("fullurl")?.asString
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun request(url: String): String? = try {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        client.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
    } catch (_: Exception) { null }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
