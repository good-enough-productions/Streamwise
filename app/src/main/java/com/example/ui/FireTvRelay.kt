package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import org.json.JSONObject

/**
 * FireTvRelay: Direct Wi-Fi launcher for Amazon Fire TV / Android TV devices.
 * 
 * Instead of battery-heavy and DRM-incompatible phone video casting, FireTvRelay
 * instructs the Fire TV to natively launch the movie inside its own installed apps
 * (Netflix, Prime Video, Hulu, Max, Tubi, etc.) in full 4K HDR.
 *
 * URL Construction Notes (from research):
 * - Netflix: Use http://www.netflix.com/watch/<id> with source=30 extra (handled server-side)
 * - Disney+: Use https://www.disneyplus.com/ app links
 * - Hulu: Use hulu:// custom scheme
 * - Most others: Use web app links (https://...)
 * - Content IDs from Watchmode API android_tv_url field (when available)
 */
object FireTvRelay {
    private const val TAG = "FireTvRelay"
    private const val DEFAULT_COMPANION_PORT = 8998
    private const val ADB_PORT = 5555

    /**
     * Build the best available deep-link URL for a provider.
     * 
     * Priority:
     * 1. If a Watchmode android_tv_url is available, use it directly
     * 2. Otherwise, construct a search/browse URL for the provider
     * 
     * Note: Netflix source=30 extra is added server-side by TvCompanionService,
     * not embedded in the URL.
     */
    fun getProviderLaunchUrl(providerId: String?, movieTitle: String, tmdbId: String?): String {
        return when (providerId?.lowercase()) {
            // Netflix: use http://www.netflix.com/watch/<id> for auto-play.
            // /watch/ auto-plays, /title/ shows detail page, /search only shows results.
            // TvCompanionService adds the source=30 extra and handles two-step cold start.
            // TODO: Replace hardcoded IDs with Watchmode android_tv_url once integrated
            "netflix" -> {
                val netflixId = resolveNetflixId(movieTitle)
                if (netflixId != null) "http://www.netflix.com/watch/$netflixId"
                else "http://www.netflix.com/search?q=${Uri.encode(movieTitle)}"
            }
            "prime", "amazon" -> "https://app.primevideo.com/search?phrase=${Uri.encode(movieTitle)}"
            "hulu" -> "https://www.hulu.com/search?q=${Uri.encode(movieTitle)}"
            "max", "hbo" -> "https://play.max.com/search?q=${Uri.encode(movieTitle)}"
            "disney" -> "https://www.disneyplus.com/search?q=${Uri.encode(movieTitle)}"
            "apple" -> "https://tv.apple.com/search?term=${Uri.encode(movieTitle)}"
            "criterion" -> "https://www.criterionchannel.com/search?q=${Uri.encode(movieTitle)}"
            "peacock" -> "https://www.peacocktv.com/watch/search?q=${Uri.encode(movieTitle)}"
            "paramount" -> "https://www.paramountplus.com/search/?q=${Uri.encode(movieTitle)}"
            "tubi" -> "https://tubitv.com/search/${Uri.encode(movieTitle)}"
            "pluto" -> "https://pluto.tv/search/details/movies/${Uri.encode(movieTitle)}"
            "freevee" -> "https://app.primevideo.com/search?phrase=${Uri.encode(movieTitle)}"
            "mubi" -> "https://mubi.com/search?query=${Uri.encode(movieTitle)}"
            "shudder" -> "https://www.shudder.com/search?q=${Uri.encode(movieTitle)}"
            "starz" -> "https://www.starz.com/us/en/search?q=${Uri.encode(movieTitle)}"
            "britbox" -> "https://www.britbox.com/us/search?q=${Uri.encode(movieTitle)}"
            else -> "https://www.google.com/search?q=${Uri.encode("$movieTitle stream")}"
        }
    }

    /**
     * Sends a command to a Fire TV via local Wi-Fi.
     * Tries:
     * 1. Streamwise Fire TV Companion HTTP listener on port 8998.
     * 2. Direct ADB Wi-Fi socket intent trigger on port 5555.
     *
     * The companion service now returns honest feedback:
     * - "playing": Playback confirmed via media session
     * - "app_opened": App opened but may need profile selection
     * - "failed": App launched but returned to home screen
     */
    suspend fun launchOnFireTv(
        fireTvIp: String,
        movieTitle: String,
        providerId: String?,
        tmdbId: String?
    ): LaunchResult = withContext(Dispatchers.IO) {
        if (fireTvIp.isBlank()) {
            return@withContext LaunchResult.Error("No Fire TV IP configured. Please set it in Settings.")
        }

        val launchUrl = getProviderLaunchUrl(providerId, movieTitle, tmdbId)

        // Attempt 1: Streamwise Companion Receiver (Port 8998)
        try {
            val url = URL("http://$fireTvIp:$DEFAULT_COMPANION_PORT/launch")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 2000
                readTimeout = 15000  // Increased: two-step Netflix launch takes ~7s
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val payload = JSONObject().apply {
                put("title", movieTitle)
                put("provider", providerId ?: "unknown")
                put("url", launchUrl)
                put("tmdbId", tmdbId ?: "")
            }.toString()

            conn.outputStream.use { it.write(payload.toByteArray()) }

            if (conn.responseCode in 200..299) {
                val respString = conn.inputStream.bufferedReader().readText()
                val respJson = try { JSONObject(respString) } catch (_: Exception) { JSONObject() }

                val status = respJson.optString("status", "unknown")
                val detail = respJson.optString("detail", "")
                val targetApp = respJson.optString("targetApp", "")

                val appLabel = when {
                    targetApp.contains("netflix") -> "Netflix"
                    targetApp.contains("hulu") -> "Hulu"
                    targetApp.contains("disney") -> "Disney+"
                    targetApp.contains("hbo") -> "Max"
                    targetApp.contains("avod") -> "Prime Video"
                    targetApp.contains("apple") -> "Apple TV"
                    targetApp.contains("peacock") -> "Peacock"
                    targetApp.contains("criterion") -> "Criterion Channel"
                    targetApp.contains("starz") -> "Starz"
                    targetApp.contains("tubi") -> "Tubi"
                    targetApp.contains("pluto") -> "Pluto TV"
                    targetApp.contains("search") -> "Universal Search"
                    else -> providerId?.replaceFirstChar { it.uppercase() } ?: "Fire TV"
                }

                Log.d(TAG, "Fire TV response: status=$status, detail=$detail, app=$appLabel")

                // Return honest feedback based on what actually happened
                return@withContext when (status) {
                    "playing" -> LaunchResult.Success(
                        "🎬 Playing \"$movieTitle\" on Danny's Fire TV via $appLabel!"
                    )
                    "app_opened" -> LaunchResult.Success(
                        "📺 Opened $appLabel on Danny's Fire TV. $detail"
                    )
                    "failed" -> LaunchResult.Error(
                        "⚠️ $appLabel opened but didn't stay active. $detail"
                    )
                    else -> LaunchResult.Success(
                        "📺 Sent \"$movieTitle\" to Danny's Fire TV via $appLabel."
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Companion receiver not responding on port 8998: ${e.message}")
        }

        // Attempt 2: Verify Fire TV port 5555
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(fireTvIp, ADB_PORT), 1500)
                Log.d(TAG, "Fire TV detected with ADB listening on 5555, but Companion on 8998 is offline.")
                return@withContext LaunchResult.Error(
                    "Fire TV detected at $fireTvIp, but Streamwise TV service is not running. Please open Streamwise on your TV once to activate."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Fire TV at $fireTvIp", e)
        }

        LaunchResult.Error("Could not reach Fire TV at $fireTvIp. Ensure it is powered on and connected to the same Wi-Fi.")
    }

    /**
     * Fallback: Launch directly on phone via Android Intent.
     */
    fun launchOnPhone(context: Context, movieTitle: String, providerId: String?, tmdbId: String?) {
        val launchUrl = getProviderLaunchUrl(providerId, movieTitle, tmdbId)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(launchUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening launch URL on phone", e)
        }
    }

    /**
     * Temporary Netflix content ID resolver for known watchlist titles.
     * TODO: Replace with Watchmode API web_url field for dynamic resolution.
     * Netflix IDs sourced from netflix.com/title/<id> URLs.
     */
    private fun resolveNetflixId(movieTitle: String): String? {
        return when (movieTitle.lowercase().trim()) {
            "malevolent" -> "80242081"
            "the laundromat" -> "80994011"
            "okja" -> "80158479"
            "side effects" -> "70243464"
            "the killing of a sacred deer" -> "80188188"
            "the lobster" -> "80044458"
            "under the skin" -> "70293812"
            "annihilation" -> "80206300"
            "midsommar" -> "81083028"
            "hereditary" -> "80199516"
            "the witch" -> "80037280"
            "it follows" -> "80013978"
            "gerald's game" -> "80128722"
            "1922" -> "80135164"
            "the ritual" -> "80217312"
            "calibre" -> "80217885"
            "apostle" -> "80158148"
            "hush" -> "80091879"
            "creep" -> "80045933"
            "creep 2" -> "80174941"
            "the invitation" -> "80048977"
            else -> null  // Falls back to search URL
        }
    }

    sealed class LaunchResult {
        data class Success(val message: String) : LaunchResult()
        data class Error(val message: String) : LaunchResult()
    }
}
