package com.example.data.local

import android.content.Context

/**
 * Manages user-configurable preferences stored in SharedPreferences.
 * Provides runtime-settable API keys that supplement (and take priority over) build-time values.
 */
class UserPreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var tmdbApiKey: String
        get() = prefs.getString(KEY_TMDB_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_TMDB_API_KEY, value.trim()).apply() }

    var ollamaHost: String
        get() = prefs.getString(KEY_OLLAMA_HOST, "192.168.86.217") ?: "192.168.86.217"
        set(value) { prefs.edit().putString(KEY_OLLAMA_HOST, value.trim()).apply() }

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_TMDB_API_KEY = "tmdb_api_key"
        private const val KEY_OLLAMA_HOST = "ollama_host"
    }
}
