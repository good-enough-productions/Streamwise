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
                    val result = triggerTvPlayback(title, provider, url, tmdbId)

                    sendResponse(out, 200, "application/json", result.toString())
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

    /**
     * Main playback trigger. Follows a strict order:
     * 1. Wake screen + HDMI-CEC pulse (BEFORE any app launch)
     * 2. Wait 500ms for CEC to settle
     * 3. Check if target app is already running (warm vs cold start)
     * 4. Launch with provider-specific intent construction
     * 5. Wait, then inspect actual result (focus, media session)
     * 6. Return honest status
     */
    private fun triggerTvPlayback(title: String, provider: String, url: String, tmdbId: String): JSONObject {
        // ---- Step 1: Wake screen + HDMI-CEC BEFORE launching any app ----
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "streamwise:wake_tv"
            )
            wl.acquire(3000)
            Log.i(TAG, "Wake lock acquired, pulsing HDMI-CEC via KEYCODE_WAKEUP")
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire wake lock: ${e.message}")
        }

        // Fire KEYCODE_WAKEUP (224) to trigger HDMI-CEC One Touch Play.
        // This MUST happen before launching any other app so we don't get
        // a SecurityException from injecting keyevents into another app's window.
        try {
            Runtime.getRuntime().exec(arrayOf("input", "keyevent", "224")).waitFor()
            Log.i(TAG, "KEYCODE_WAKEUP sent for HDMI-CEC")
        } catch (e: Exception) {
            Log.w(TAG, "KEYCODE_WAKEUP failed: ${e.message}")
        }

        // Wait for CEC to settle and TV input to switch
        Thread.sleep(500)

        // ---- Step 2: Map provider to Fire TV package ----
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

        // ---- Step 3: Launch ----
        val launchedName: String
        val pkgMgr = packageManager

        if (targetPackage != null && isAppInstalled(pkgMgr, targetPackage)) {
            if (targetPackage == "com.netflix.ninja" && url.isNotBlank()) {
                // Netflix-specific: two-step launch with source=30 extra
                launchedName = launchNetflix(pkgMgr, url, title)
            } else if (url.isNotBlank()) {
                // Other providers: standard VIEW intent with LEANBACK_LAUNCHER
                launchedName = launchGenericProvider(pkgMgr, targetPackage, url)
            } else {
                // No URL: just open the app
                val launchIntent = pkgMgr.getLaunchIntentForPackage(targetPackage)?.apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                }
                if (launchIntent != null) {
                    startActivity(launchIntent)
                    launchedName = targetPackage
                    Log.i(TAG, "Launched app generically: $targetPackage")
                } else {
                    startSearchFallback(title)
                    launchedName = "search"
                }
            }
        } else {
            startSearchFallback(title)
            launchedName = "search"
        }

        // ---- Step 4: Wait and inspect result ----
        Thread.sleep(2500)
        val feedback = inspectResult(launchedName, title)

        return JSONObject().apply {
            put("status", feedback.status)
            put("title", title)
            put("targetApp", launchedName)
            put("detail", feedback.detail)
        }
    }

    /**
     * Netflix-specific launch with the required source="30" extra.
     *
     * Research findings:
     * - Netflix on Android TV REQUIRES the string extra source="30" to process deep links
     * - DO NOT use FLAG_ACTIVITY_CLEAR_TASK — it crashes Netflix's Widevine DRM
     * - On cold start, Netflix drops the deep link during initialization.
     *   Workaround: check if Netflix is running, if not, launch generically first,
     *   wait for initialization, then resend the deep link.
     * - Use http:// scheme (not nflx:// or netflix://) with LEANBACK_LAUNCHER category
     */
    private fun launchNetflix(pkgMgr: PackageManager, url: String, title: String): String {
        val netflixPkg = "com.netflix.ninja"

        // Normalize URL to http scheme for Netflix TV (most reliable per community docs)
        val httpUrl = url
            .replace("nflx://www.netflix.com/", "http://www.netflix.com/")
            .replace("netflix://", "http://www.netflix.com/")

        // Check if Netflix is already running (warm start vs cold start)
        val isRunning = isAppInForegroundOrRecent(netflixPkg)

        if (!isRunning) {
            // Cold start: launch Netflix generically first to get past initialization + profile picker
            Log.i(TAG, "Netflix cold start detected. Launching generically first...")
            val genericIntent = pkgMgr.getLaunchIntentForPackage(netflixPkg)?.apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (genericIntent != null) {
                startActivity(genericIntent)
                // Wait for Netflix to initialize and pass profile picker
                Thread.sleep(4000)
                Log.i(TAG, "Netflix initialized. Now sending deep link: $httpUrl")
            }
        }

        // Send the deep link intent with source="30"
        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(netflixPkg, "$netflixPkg.MainActivity")
            data = Uri.parse(httpUrl)
            putExtra("source", "30")  // REQUIRED: tells Netflix this is an external launch
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK  // NO CLEAR_TASK — crashes Widevine DRM
        }

        try {
            startActivity(deepLinkIntent)
            Log.i(TAG, "Netflix deep link sent: $httpUrl with source=30")
        } catch (e: Exception) {
            Log.e(TAG, "Netflix deep link failed: ${e.message}. Falling back to generic launch.")
            val fallback = pkgMgr.getLaunchIntentForPackage(netflixPkg)
            if (fallback != null) startActivity(fallback)
        }

        return netflixPkg
    }

    /**
     * Generic provider launch with LEANBACK_LAUNCHER category.
     * Uses standard VIEW intent without aggressive flags.
     */
    private fun launchGenericProvider(pkgMgr: PackageManager, targetPackage: String, url: String): String {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(targetPackage)
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val launchIntent = if (viewIntent.resolveActivity(pkgMgr) != null) {
            viewIntent
        } else {
            // Deep link URI not handled — fall back to just opening the app
            Log.w(TAG, "Deep link URI not resolved for $targetPackage, falling back to generic launch")
            pkgMgr.getLaunchIntentForPackage(targetPackage)?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }
        }

        if (launchIntent != null) {
            startActivity(launchIntent)
            Log.i(TAG, "Launched $targetPackage with url: $url")
        }

        return targetPackage
    }

    /**
     * Check if an app is currently in the foreground or recent task stack.
     * Used to determine cold vs warm start for Netflix two-step launch.
     */
    private fun isAppInForegroundOrRecent(packageName: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("dumpsys", "activity", "recents"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "Could not check recents for $packageName: ${e.message}")
            false
        }
    }

    /**
     * After launching, inspect the actual result on the TV to report honest feedback.
     * Checks window focus and media session state.
     */
    private fun inspectResult(targetApp: String, title: String): LaunchFeedback {
        try {
            // Check which window has focus
            val focusProcess = Runtime.getRuntime().exec(arrayOf("dumpsys", "window"))
            val focusOutput = focusProcess.inputStream.bufferedReader().readText()
            focusProcess.waitFor()

            val focusMatch = Regex("mCurrentFocus=Window\\{[^ ]+ [^ ]+ ([^}]+)\\}").find(focusOutput)
            val currentFocus = focusMatch?.groupValues?.get(1) ?: "unknown"
            Log.i(TAG, "Current window focus: $currentFocus")

            // Check media session playback state
            val mediaProcess = Runtime.getRuntime().exec(arrayOf("dumpsys", "media_session"))
            val mediaOutput = mediaProcess.inputStream.bufferedReader().readText()
            mediaProcess.waitFor()

            val isPlaying = mediaOutput.contains("package=$targetApp") &&
                    mediaOutput.contains("state=3") // PlaybackState.STATE_PLAYING = 3

            return when {
                isPlaying -> {
                    Log.i(TAG, "✅ Playback confirmed for $targetApp")
                    LaunchFeedback("playing", "Playback active for \"$title\"")
                }
                currentFocus.contains(targetApp) -> {
                    Log.i(TAG, "⚠️ App opened but not playing: $currentFocus")
                    LaunchFeedback("app_opened", "Opened on TV — may need profile selection or manual play")
                }
                currentFocus.contains("launcher") || currentFocus.contains("com.amazon.tv") -> {
                    Log.w(TAG, "❌ App did not stay in foreground. Focus is on: $currentFocus")
                    LaunchFeedback("failed", "App launched but returned to home screen")
                }
                else -> {
                    LaunchFeedback("app_opened", "TV focus: $currentFocus")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not inspect result: ${e.message}")
            return LaunchFeedback("unknown", "Launched but could not verify result")
        }
    }

    private data class LaunchFeedback(val status: String, val detail: String)

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
