package com.sikamikaniko.sonora.data

import android.content.Context

/** Minimal persisted config: which server to talk to and the credentials. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("sonora", Context.MODE_PRIVATE)

    var baseUrl: String?
        get() = sp.getString("baseUrl", null)
        set(v) = sp.edit().putString("baseUrl", v).apply()

    var username: String?
        get() = sp.getString("username", null)
        set(v) = sp.edit().putString("username", v).apply()

    var password: String?
        get() = sp.getString("password", null)
        set(v) = sp.edit().putString("password", v).apply()

    var dynamicColor: Boolean
        get() = sp.getBoolean("dynamicColor", false)
        set(v) = sp.edit().putBoolean("dynamicColor", v).apply()

    var artTheme: Boolean
        get() = sp.getBoolean("artTheme", true)
        set(v) = sp.edit().putBoolean("artTheme", v).apply()

    /** Selected Aurora theme, stored by AppTheme.name (default "MIDNIGHT"). */
    var themeName: String
        get() = sp.getString("themeName", "MIDNIGHT") ?: "MIDNIGHT"
        set(v) = sp.edit().putString("themeName", v).apply()

    // ---- AI (all user-configured; nothing hardcoded) ----
    var aiEnabled: Boolean
        get() = sp.getBoolean("aiEnabled", false)
        set(v) = sp.edit().putBoolean("aiEnabled", v).apply()

    /** Ollama-compatible base URL, e.g. http://your-server:11434 */
    var aiBaseUrl: String
        get() = sp.getString("aiBaseUrl", "") ?: ""
        set(v) = sp.edit().putString("aiBaseUrl", v).apply()

    var aiModel: String
        get() = sp.getString("aiModel", "") ?: ""
        set(v) = sp.edit().putString("aiModel", v).apply()

    /** Target language for lyric translation. */
    var aiLang: String
        get() = sp.getString("aiLang", "English") ?: "English"
        set(v) = sp.edit().putString("aiLang", v).apply()

    /** Recent search queries, most-recent first (newline-delimited). */
    var recentSearches: List<String>
        get() = sp.getString("recentSearches", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        set(v) = sp.edit().putString("recentSearches", v.joinToString("\n")).apply()

    val isConfigured: Boolean
        get() = !baseUrl.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()

    fun save(baseUrl: String, username: String, password: String) {
        sp.edit()
            .putString("baseUrl", baseUrl)
            .putString("username", username)
            .putString("password", password)
            .apply()
    }

    fun clear() = sp.edit().clear().apply()
}
