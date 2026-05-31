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

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_TMDB_API_KEY = "tmdb_api_key"
    }
}
