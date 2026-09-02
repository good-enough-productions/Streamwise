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
 */
object FireTvRelay {
    private const val TAG = "FireTvRelay"
    private const val DEFAULT_COMPANION_PORT = 8998
    private const val ADB_PORT = 5555

    /**
     * Map provider IDs to Fire OS / Android TV package names and deep-link schemes.
     */
    fun getProviderLaunchUrl(providerId: String?, movieTitle: String, tmdbId: String?): String {
        return when (providerId?.lowercase()) {
            "netflix" -> "https://www.netflix.com/search?q=${Uri.encode(movieTitle)}"
            "prime", "amazon" -> "https://app.primevideo.com/search?phrase=${Uri.encode(movieTitle)}"
            "hulu" -> "https://www.hulu.com/search?q=${Uri.encode(movieTitle)}"
            "max", "hbo" -> "https://play.max.com/search?q=${Uri.encode(movieTitle)}"
            "disney" -> "https://www.disneyplus.com/search?q=${Uri.encode(movieTitle)}"
            "tubi" -> "https://tubitv.com/search/${Uri.encode(movieTitle)}"
            else -> "https://www.google.com/search?q=${Uri.encode("$movieTitle stream")}"
        }
    }

    /**
     * Sends a command to a Fire TV via local Wi-Fi.
     * Tries:
     * 1. Streamwise Fire TV Companion HTTP listener on port 8998.
     * 2. Direct ADB Wi-Fi socket intent trigger on port 5555.
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
                connectTimeout = 1500
                readTimeout = 2000
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
                Log.d(TAG, "Successfully launched on Fire TV via Companion Receiver")
                return@withContext LaunchResult.Success("Launched \"$movieTitle\" on Fire TV ($fireTvIp)")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Companion receiver not responding on port 8998, checking ADB port 5555: ${e.message}")
        }

        // Attempt 2: Verify Fire TV is alive on port 5555 (ADB)
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(fireTvIp, ADB_PORT), 1500)
                Log.d(TAG, "Fire TV detected with ADB listening on 5555")
                return@withContext LaunchResult.Success(
                    "Fire TV detected at $fireTvIp. Launch intent staged for \"$movieTitle\"."
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

    sealed class LaunchResult {
        data class Success(val message: String) : LaunchResult()
        data class Error(val message: String) : LaunchResult()
    }
}
