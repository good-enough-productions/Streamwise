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

    var githubToken: String
        get() = prefs.getString(KEY_GITHUB_TOKEN, "") ?: ""
        set(value) { prefs.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply() }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) { prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply() }

    var watchmodeApiKey: String
        get() = prefs.getString(KEY_WATCHMODE_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WATCHMODE_API_KEY, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var enableBetaFeedback: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_BETA_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_BETA_FEEDBACK, value).apply()

    var googleSheetWebhookUrl: String
        get() = prefs.getString(KEY_GOOGLE_SHEET_WEBHOOK_URL, "https://script.google.com/macros/s/AKfycbzsbZfiDbXXGJunAmJX2xb9OtpnigwVl69M6qbBQ5bNBuyAdj6TtkW-LflbSxSFJJoI0w/exec") ?: "https://script.google.com/macros/s/AKfycbzsbZfiDbXXGJunAmJX2xb9OtpnigwVl69M6qbBQ5bNBuyAdj6TtkW-LflbSxSFJJoI0w/exec"
        set(value) = prefs.edit().putString(KEY_GOOGLE_SHEET_WEBHOOK_URL, value.trim()).apply()

    var isSpotlightCollapsed: Boolean
        get() = prefs.getBoolean(KEY_SPOTLIGHT_COLLAPSED, false)
        set(value) = prefs.edit().putBoolean(KEY_SPOTLIGHT_COLLAPSED, value).apply()

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_TMDB_API_KEY = "tmdb_api_key"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_WATCHMODE_API_KEY = "watchmode_api_key"
        private const val KEY_OLLAMA_HOST = "ollama_host"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ENABLE_BETA_FEEDBACK = "enable_beta_feedback"
        private const val KEY_GOOGLE_SHEET_WEBHOOK_URL = "google_sheet_webhook_url"
        private const val KEY_SPOTLIGHT_COLLAPSED = "spotlight_collapsed"
    }
}
