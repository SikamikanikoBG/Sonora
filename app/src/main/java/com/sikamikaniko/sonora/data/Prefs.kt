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
