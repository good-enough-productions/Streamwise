package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val title: String,
    val downloadUrl: String,
    val isNewer: Boolean
)

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    const val CURRENT_VERSION = "v1.6.2"

    suspend fun checkLatestRelease(repo: String = "good-enough-productions/Streamwise"): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("https://api.github.com/repos/$repo/releases/latest")
            val conn = endpoint.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "Streamwise-UpdateChecker")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            if (conn.responseCode == 200) {
                val rawJson = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(rawJson)
                val tagName = json.optString("tag_name", "")
                val name = json.optString("name", tagName)

                var apkDownloadUrl = json.optString("html_url", "")
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", apkDownloadUrl)
                            break
                        }
                    }
                }

                val isNewer = tagName.isNotBlank() && tagName != CURRENT_VERSION
                return@withContext UpdateInfo(
                    tagName = tagName,
                    title = name,
                    downloadUrl = apkDownloadUrl,
                    isNewer = isNewer
                )
            } else {
                Log.d(TAG, "No releases found or status: ${conn.responseCode}")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Update check failed: ${ex.message}")
        }
        null
    }

    fun launchDownload(context: Context, downloadUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch update url: ${e.message}")
        }
    }
}
