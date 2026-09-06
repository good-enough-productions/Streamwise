package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpdateAvailable(
        val tagName: String,
        val releaseName: String,
        val releaseNotes: String,
        val apkDownloadUrl: String,
        val apkSize: Long
    ) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class GitHubUpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val repoOwner = "good-enough-productions"
    private val repoNames = listOf("Streamwise", "master-hub")

    suspend fun checkForUpdates(currentVersionName: String): UpdateStatus = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        try {
            for (repo in repoNames) {
                val url = "https://api.github.com/repos/$repoOwner/$repo/releases"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "Streamwise-App")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val releases = JSONArray(body)
                            if (releases.length() > 0) {
                                val latest = releases.getJSONObject(0)
                                val tagName = latest.optString("tag_name", "")
                                val releaseName = latest.optString("name", tagName)
                                val releaseNotes = latest.optString("body", "")
                                val assets = latest.optJSONArray("assets")

                                var apkUrl: String? = null
                                var apkSize: Long = 0L

                                if (assets != null) {
                                    for (i in 0 until assets.length()) {
                                        val asset = assets.getJSONObject(i)
                                        val assetName = asset.optString("name", "")
                                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                                            apkUrl = asset.optString("browser_download_url", "")
                                            apkSize = asset.optLong("size", 0L)
                                            break
                                        }
                                    }
                                }

                                if (isNewerVersion(tagName, currentVersionName) && !apkUrl.isNullOrEmpty()) {
                                    val status = UpdateStatus.UpdateAvailable(
                                        tagName = tagName,
                                        releaseName = releaseName,
                                        releaseNotes = releaseNotes,
                                        apkDownloadUrl = apkUrl,
                                        apkSize = apkSize
                                    )
                                    _updateStatus.value = status
                                    return@withContext status
                                }
                            }
                        }
                    }
                }
            }
            _updateStatus.value = UpdateStatus.UpToDate
            UpdateStatus.UpToDate
        } catch (e: Exception) {
            Log.e("GitHubUpdateManager", "Error checking for updates: ${e.message}", e)
            val error = UpdateStatus.Error("Failed to check for updates: ${e.localizedMessage}")
            _updateStatus.value = error
            error
        }
    }

    suspend fun downloadAndInstallApk(downloadUrl: String, fileName: String = "streamwise-update.apk") = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Downloading(0, 0, 0)
            val request = Request.Builder().url(downloadUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Download failed with HTTP ${response.code}")
                }
                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                val apkDir = File(context.cacheDir, "updates")
                if (!apkDir.exists()) apkDir.mkdirs()
                val apkFile = File(apkDir, fileName)
                if (apkFile.exists()) apkFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead: Long = 0
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            val percent = if (contentLength > 0) ((totalRead * 100) / contentLength).toInt() else 0
                            _updateStatus.value = UpdateStatus.Downloading(percent, totalRead, contentLength)
                        }
                        output.flush()
                    }
                }
                _updateStatus.value = UpdateStatus.ReadyToInstall(apkFile)
                withContext(Dispatchers.Main) {
                    triggerPackageInstaller(apkFile)
                }
            }
        } catch (e: Exception) {
            Log.e("GitHubUpdateManager", "Error downloading APK: ${e.message}", e)
            _updateStatus.value = UpdateStatus.Error("Download failed: ${e.localizedMessage}")
        }
    }

    fun triggerPackageInstaller(apkFile: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("GitHubUpdateManager", "Failed to launch installer: ${e.message}", e)
            _updateStatus.value = UpdateStatus.Error("Failed to trigger installer: ${e.localizedMessage}")
        }
    }

    fun resetStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    private fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.replace(Regex("[^0-9.]"), "")
        val cleanCurrent = currentVersion.replace(Regex("[^0-9.]"), "")
        if (cleanRemote.isBlank() || cleanCurrent.isBlank()) return false
        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
