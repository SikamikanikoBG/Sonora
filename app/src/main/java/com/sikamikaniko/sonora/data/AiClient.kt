package com.sikamikaniko.sonora.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Minimal client for an Ollama-compatible server (fully user-configured — no
 * endpoint is hardcoded). Supports listing models, one-shot chat (optionally
 * JSON-forced) and token streaming for a live, fast-feeling response.
 */
object AiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val JSON = "application/json".toMediaType()

    data class Msg(val role: String, val content: String)

    private fun base(baseUrl: String) = baseUrl.trim().trimEnd('/')

    /** GET /api/tags — the models installed on the server. */
    suspend fun listModels(baseUrl: String): List<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext emptyList()
        try {
            val req = Request.Builder().url("${base(baseUrl)}/api/tags").build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext emptyList()
                val obj = JsonParser.parseString(r.body?.string()).asJsonObject
                obj.getAsJsonArray("models")?.mapNotNull {
                    it.asJsonObject.get("name")?.asString
                } ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** One-shot chat. Set [json] to force a JSON object reply. */
    suspend fun chat(baseUrl: String, model: String, messages: List<Msg>, json: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank() || model.isBlank()) return@withContext null
            try {
                val payload = mutableMapOf<String, Any>(
                    "model" to model,
                    "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
                    "stream" to false,
                    "options" to mapOf("num_ctx" to 8192, "temperature" to 0.4)
                )
                if (json) payload["format"] = "json"
                val body = gson.toJson(payload).toRequestBody(JSON)
                val req = Request.Builder().url("${base(baseUrl)}/api/chat").post(body).build()
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) return@withContext null
                    val obj = JsonParser.parseString(r.body?.string()).asJsonObject
                    obj.getAsJsonObject("message")?.get("content")?.asString
                }
            } catch (e: Exception) {
                null
            }
        }

    /** Streaming chat — [onToken] is invoked for each chunk as it arrives. Returns true on success. */
    suspend fun chatStream(
        baseUrl: String,
        model: String,
        messages: List<Msg>,
        onToken: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank() || model.isBlank()) return@withContext false
        try {
            val payload = mapOf(
                "model" to model,
                "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
                "stream" to true,
                "options" to mapOf("num_ctx" to 8192, "temperature" to 0.6)
            )
            val body = gson.toJson(payload).toRequestBody(JSON)
            val req = Request.Builder().url("${base(baseUrl)}/api/chat").post(body).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext false
                val source = r.body?.source() ?: return@withContext false
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    val obj = JsonParser.parseString(line).asJsonObject
                    obj.getAsJsonObject("message")?.get("content")?.asString?.let { onToken(it) }
                    if (obj.get("done")?.asBoolean == true) break
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
