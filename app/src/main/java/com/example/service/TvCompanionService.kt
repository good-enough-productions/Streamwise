package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * TvCompanionService: Zero-latency Wi-Fi HTTP listener on port 8998.
 * Runs on Fire TV / Android TV to receive trigger commands from Streamwise on the phone.
 * Launches native TV apps (Netflix, Hulu, Prime Video, Disney+, Max, Tubi, etc.) or universal search.
 */
class TvCompanionService : Service() {

    companion object {
        private const val TAG = "TvCompanionService"
        const val PORT = 8998
        private var isServerRunning = false

        fun start(context: Context) {
            val intent = Intent(context, TvCompanionService::class.java)
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service: ${e.message}")
            }
        }
    }

    private var serverSocket: ServerSocket? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TvCompanionService created.")
        startServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TvCompanionService onStartCommand.")
        startServer()
        return START_STICKY
    }

    @Synchronized
    private fun startServer() {
        if (isServerRunning) return
        isServerRunning = true

        thread(isDaemon = true, name = "TvCompanionHttpServer") {
            try {
                serverSocket = ServerSocket(PORT)
                Log.i(TAG, "TvCompanionServer listening on port $PORT")
                while (isServerRunning && serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: break
                    thread { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket error: ${e.message}")
            } finally {
                isServerRunning = false
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()

            val requestLine = reader.readLine() ?: return
            Log.d(TAG, "Request: $requestLine")

            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                if (line!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(chars, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                String(chars, 0, read)
            } else ""

            val parts = requestLine.split(" ")
            val path = if (parts.size > 1) parts[1] else "/"

            when {
                path == "/ping" || path == "/status" -> {
                    sendResponse(out, 200, "application/json", "{\"status\":\"online\",\"model\":\"${Build.MODEL}\"}")
                }
                path == "/launch" -> {
                    val json = JSONObject(body.ifBlank { "{}" })
                    val title = json.optString("title", "")
                    val provider = json.optString("provider", "")
                    val url = json.optString("url", "")
                    val tmdbId = json.optString("tmdbId", "")

                    Log.i(TAG, "Launching on TV: title=$title, provider=$provider, url=$url")
                    val launchedApp = triggerTvPlayback(title, provider, url, tmdbId)

                    val resp = JSONObject().apply {
                        put("status", "launched")
                        put("title", title)
                        put("targetApp", launchedApp)
                    }
                    sendResponse(out, 200, "application/json", resp.toString())
                }
                else -> {
                    sendResponse(out, 404, "text/plain", "Not Found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client: ${e.message}")
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun triggerTvPlayback(title: String, provider: String, url: String, tmdbId: String): String {
        // 1. Wake screen & Pulse HDMI-CEC to power TV glass
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "streamwise:wake_tv"
            )
            wl.acquire(3000)
            // Fire KEYCODE_WAKEUP to trigger HDMI-CEC One Touch Play across the wire
            Runtime.getRuntime().exec("input keyevent 224")
        } catch (e: Exception) {
            Log.w(TAG, "Could not wake screen or pulse CEC: ${e.message}")
        }

        // 2. Map provider to Fire TV package
        val targetPackage = when (provider.lowercase()) {
            "netflix" -> "com.netflix.ninja"
            "hulu" -> "com.hulu.plus"
            "disney", "disneyplus" -> "com.disney.disneyplus"
            "hbo", "max" -> "com.hbo.hbonow"
            "prime", "prime_video", "amazon" -> "com.amazon.avod"
            "apple", "appletv" -> "com.apple.atve.amazon.appletv"
            "peacock" -> "com.peacock.peacockfiretv"
            "criterion" -> "com.criterionchannel"
            "starz" -> "com.starz.starzplay.firetv"
            "tubi" -> "com.tubitv.ott"
            "pluto" -> "tv.pluto.android"
            "plex" -> "com.plexapp.android"
            "youtube" -> "com.amazon.firetv.youtube"
            else -> null
        }

        // 3. Launch App or Universal Search
        val launchedName: String
        val pm = packageManager

        if (targetPackage != null && isAppInstalled(pm, targetPackage)) {
            // Priority 1: Launch target provider with deep-link URI
            val viewIntent = if (url.isNotBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage(targetPackage)
                    // For Netflix on TV, CLEAR_TASK forces delivery of deep-link past the profile selection screen
                    flags = if (targetPackage == "com.netflix.ninja") {
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    } else {
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                }
            } else null

            val launchIntent = viewIntent?.takeIf { it.resolveActivity(pm) != null }
                ?: pm.getLaunchIntentForPackage(targetPackage)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }

            if (launchIntent != null) {
                startActivity(launchIntent)
                launchedName = targetPackage
                Log.i(TAG, "Started activity for package: $targetPackage with url: $url")
            } else {
                startSearchFallback(title)
                launchedName = "search"
            }
        } else {
            // Priority 2: Universal Fire TV search intent for the title
            startSearchFallback(title)
            launchedName = "search"
        }

        return launchedName
    }

    private fun startSearchFallback(title: String) {
        try {
            val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
                putExtra("query", title)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(searchIntent)
            Log.i(TAG, "Triggered universal search for \"$title\"")
        } catch (e: Exception) {
            Log.e(TAG, "Universal search failed: ${e.message}")
        }
    }

    private fun isAppInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun sendResponse(out: OutputStream, code: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val statusText = if (code == 200) "OK" else "Not Found"
        val header = "HTTP/1.1 $code $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServerRunning = false
        try { serverSocket?.close() } catch (_: Exception) {}
    }
}
