package com.sikamikaniko.sonora.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Minimal client for a self-hosted LLM server (fully user-configured — no
 * endpoint is hardcoded). Speaks either Ollama's native API or the
 * OpenAI-compatible API served by vLLM. Supports listing models, one-shot chat
 * (optionally JSON-forced) and token streaming for a live, fast-feeling response.
 */
object AiClient {

    enum class Provider(val label: String, val urlHint: String) {
        OLLAMA("Ollama", "http://your-server:11434"),
        VLLM("vLLM", "http://your-server:8000");

        companion object {
            fun of(name: String?) = values().firstOrNull { it.name == name } ?: OLLAMA
        }
    }

    /** Which API to speak, and the optional bearer token (vLLM `--api-key`). Set from Settings. */
    @Volatile var provider: Provider = Provider.OLLAMA
    @Volatile var apiKey: String = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        // Per-read timeout: a healthy model streams tokens well within this; a hung
        // or unreachable one now fails in a minute instead of leaving the user waiting.
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val JSON = "application/json".toMediaType()

    data class Msg(val role: String, val content: String)

    private fun base(baseUrl: String) = baseUrl.trim().trimEnd('/')

    /** OpenAI-style root — accepts the URL with or without a trailing /v1. */
    private fun v1(baseUrl: String) = base(baseUrl).removeSuffix("/v1") + "/v1"

    private val vllm get() = provider == Provider.VLLM

    private fun chatUrl(baseUrl: String) =
        if (vllm) "${v1(baseUrl)}/chat/completions" else "${base(baseUrl)}/api/chat"

    private fun request(url: String): Request.Builder = Request.Builder().url(url).apply {
        val key = apiKey.trim()
        if (vllm && key.isNotEmpty()) header("Authorization", "Bearer $key")
    }

    private fun chatPayload(model: String, messages: List<Msg>, stream: Boolean, temperature: Double, json: Boolean): Map<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "model" to model,
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
            "stream" to stream
        )
        if (vllm) {
            payload["temperature"] = temperature
            // Turns off reasoning on thinking-capable chat templates (Qwen3 etc.); ignored otherwise.
            payload["chat_template_kwargs"] = mapOf("enable_thinking" to false)
            if (json) payload["response_format"] = mapOf("type" to "json_object")
        } else {
            payload["think"] = false // disable reasoning tokens on thinking-capable models (ignored otherwise)
            payload["options"] = mapOf("num_ctx" to 8192, "temperature" to temperature)
            if (json) payload["format"] = "json"
        }
        return payload
    }

    /** The reply text from a full response (`message`) or a stream chunk (`delta` on vLLM). */
    private fun content(obj: JsonObject, streaming: Boolean): String? {
        val el: JsonElement? = if (vllm) {
            obj.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
                ?.getAsJsonObject(if (streaming) "delta" else "message")?.get("content")
        } else {
            obj.getAsJsonObject("message")?.get("content")
        }
        return el?.takeUnless { it.isJsonNull }?.asString
    }

    /** Strips <think>/<thinking> reasoning blocks (including an unclosed trailing one). */
    private fun stripThink(s: String): String {
        var r = s.replace(Regex("(?s)<think>.*?</think>"), "")
            .replace(Regex("(?s)<thinking>.*?</thinking>"), "")
        var open = r.indexOf("<think>")
        if (open < 0) open = r.indexOf("<thinking>")
        if (open >= 0) r = r.substring(0, open)
        return r.trimStart()
    }

    /** The models the server offers — GET /api/tags (Ollama) or /v1/models (vLLM). */
    suspend fun listModels(baseUrl: String): List<String> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) return@withContext emptyList()
        try {
            val url = if (vllm) "${v1(baseUrl)}/models" else "${base(baseUrl)}/api/tags"
            client.newCall(request(url).build()).execute().use { r ->
                if (!r.isSuccessful) return@withContext emptyList()
                val obj = JsonParser.parseString(r.body?.string()).asJsonObject
                obj.getAsJsonArray(if (vllm) "data" else "models")?.mapNotNull {
                    it.asJsonObject.get(if (vllm) "id" else "name")?.asString
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
                // JSON calls are extraction/index-picking — classification, not writing;
                // sampling noise there means off-vocab genres and invalid indices.
                val payload = chatPayload(model, messages, stream = false, temperature = if (json) 0.0 else 0.4, json = json)
                val body = gson.toJson(payload).toRequestBody(JSON)
                val req = request(chatUrl(baseUrl)).post(body).build()
                client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) return@withContext null
                    val obj = JsonParser.parseString(r.body?.string()).asJsonObject
                    content(obj, streaming = false)?.let { stripThink(it) }
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
            val payload = chatPayload(model, messages, stream = true, temperature = 0.6, json = false)
            val body = gson.toJson(payload).toRequestBody(JSON)
            val req = request(chatUrl(baseUrl)).post(body).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext false
                val source = r.body?.source() ?: return@withContext false
                val raw = StringBuilder()
                var emitted = 0
                while (!source.exhausted()) {
                    // Stop promptly if the caller's coroutine was cancelled (e.g. the user
                    // navigated away) so a stale stream can't keep writing to shared state.
                    if (!isActive) return@withContext false
                    var line = source.readUtf8Line() ?: break
                    if (line.isBlank()) continue
                    if (vllm) {
                        // Server-sent events: "data: {...}" lines, ended by "data: [DONE]".
                        if (!line.startsWith("data:")) continue
                        line = line.removePrefix("data:").trim()
                        if (line == "[DONE]") break
                    }
                    val obj = JsonParser.parseString(line).asJsonObject
                    content(obj, streaming = true)?.let { chunk ->
                        // Suppress any <think> reasoning; only stream the real answer.
                        raw.append(chunk)
                        val cleaned = stripThink(raw.toString())
                        if (cleaned.length > emitted) {
                            onToken(cleaned.substring(emitted))
                            emitted = cleaned.length
                        }
                    }
                    if (!vllm && obj.get("done")?.asBoolean == true) break
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
