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

    var watchmodeApiKey: String
        get() = prefs.getString(KEY_WATCHMODE_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WATCHMODE_API_KEY, value).apply()

    var googleSheetWebhookUrl: String
        get() = prefs.getString(KEY_GOOGLE_SHEET_WEBHOOK_URL, DEFAULT_GOOGLE_SHEET_WEBHOOK_URL) ?: DEFAULT_GOOGLE_SHEET_WEBHOOK_URL
        set(value) = prefs.edit().putString(KEY_GOOGLE_SHEET_WEBHOOK_URL, value.trim()).apply()

    var fireTvIp: String
        get() = prefs.getString(KEY_FIRE_TV_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FIRE_TV_IP, value.trim()).apply()

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_TMDB_API_KEY = "tmdb_api_key"
        private const val KEY_WATCHMODE_API_KEY = "watchmode_api_key"
        private const val KEY_OLLAMA_HOST = "ollama_host"
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_GOOGLE_SHEET_WEBHOOK_URL = "google_sheet_webhook_url"
        private const val KEY_FIRE_TV_IP = "fire_tv_ip"

        // Canonical Google Apps Script Webhook URL deployed via clasp ($0/mo)
        const val DEFAULT_GOOGLE_SHEET_WEBHOOK_URL = "https://script.google.com/macros/s/AKfycbwTFjzb2NgW_Py8dhNTWY1Qen9y4D93yG0NUvzhkm1jzKfCz_gE01WQryMcNThfSXEKqQ/exec"
    }
}
