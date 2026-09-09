package com.example.data.util

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Utility to eliminate couch friction by launching streaming provider apps directly on Android.
 * Handles known package names, deep links, Google Play Store fallbacks, and universal media search.
 */
object StreamingAppLauncher {

    private const val TAG = "StreamingAppLauncher"

    data class LaunchResult(
        val success: Boolean,
        val providerName: String,
        val message: String
    )

    data class ProviderLaunchConfig(
        val providerId: String,
        val displayName: String,
        val packageNames: List<String>,
        val searchUrl: (title: String) -> String,
        val deepLinkScheme: String? = null
    )

    private val PROVIDERS = listOf(
        ProviderLaunchConfig(
            providerId = "prime",
            displayName = "Prime Video",
            packageNames = listOf("com.amazon.avod.thirdpartyclient", "com.amazon.amazonvideo.livingroom"),
            searchUrl = { "https://app.primevideo.com/watch?phrase=" + Uri.encode(it) },
            deepLinkScheme = "primevideo://"
        ),
        ProviderLaunchConfig(
            providerId = "netflix",
            displayName = "Netflix",
            packageNames = listOf("com.netflix.mediaclient", "com.netflix.ninja"),
            searchUrl = { "https://www.netflix.com/search?q=" + Uri.encode(it) },
            deepLinkScheme = "nflx://"
        ),
        ProviderLaunchConfig(
            providerId = "max",
            displayName = "Max",
            packageNames = listOf("com.wbd.stream", "com.hbo.hbonow"),
            searchUrl = { "https://play.max.com/search?q=" + Uri.encode(it) },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "disney",
            displayName = "Disney+",
            packageNames = listOf("com.disney.disneyplus"),
            searchUrl = { "https://www.disneyplus.com/search?q=" + Uri.encode(it) },
            deepLinkScheme = "disneyplus://"
        ),
        ProviderLaunchConfig(
            providerId = "apple",
            displayName = "Apple TV+",
            packageNames = listOf("com.apple.atve.androidtv.appletv"),
            searchUrl = { "https://tv.apple.com/search?term=" + Uri.encode(it) },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "hulu",
            displayName = "Hulu",
            packageNames = listOf("com.hulu.plus"),
            searchUrl = { "https://www.hulu.com/search?q=" + Uri.encode(it) },
            deepLinkScheme = "hulu://"
        ),
        ProviderLaunchConfig(
            providerId = "tubi",
            displayName = "Tubi",
            packageNames = listOf("com.tubitv"),
            searchUrl = { "https://tubitv.com/search/" + Uri.encode(it) },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "pluto",
            displayName = "Pluto TV",
            packageNames = listOf("tv.pluto.android"),
            searchUrl = { "https://pluto.tv/search" },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "peacock",
            displayName = "Peacock",
            packageNames = listOf("com.peacocktv.peacockandroid"),
            searchUrl = { "https://www.peacocktv.com" },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "paramount",
            displayName = "Paramount+",
            packageNames = listOf("com.cbs.app"),
            searchUrl = { "https://www.paramountplus.com/search/?q=" + Uri.encode(it) },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "criterion",
            displayName = "Criterion Channel",
            packageNames = listOf("com.vhx.criterionchannel"),
            searchUrl = { "https://www.criterionchannel.com/search?q=" + Uri.encode(it) },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "mubi",
            displayName = "MUBI",
            packageNames = listOf("com.mubi"),
            searchUrl = { "https://mubi.com/en/search/films?query=" + Uri.encode(it) },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "freevee",
            displayName = "Freevee",
            packageNames = listOf("com.amazon.imdb.tv.android.app", "com.amazon.avod.thirdpartyclient"),
            searchUrl = { "https://app.primevideo.com/watch?phrase=" + Uri.encode(it) },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "kanopy",
            displayName = "Kanopy",
            packageNames = listOf("com.kanopy"),
            searchUrl = { "https://www.kanopy.com/en/search?query=" + Uri.encode(it) },
            deepLinkScheme = null
        ),
        ProviderLaunchConfig(
            providerId = "hoopla",
            displayName = "Hoopla",
            packageNames = listOf("com.hoopladigital.android"),
            searchUrl = { "https://www.hoopladigital.com/search?q=" + Uri.encode(it) },
            deepLinkScheme = null
        )
    )

    fun findConfig(providerId: String): ProviderLaunchConfig? {
        val cleanId = providerId.trim().lowercase()
        return PROVIDERS.firstOrNull { 
            cleanId == it.providerId || cleanId.contains(it.providerId) || it.providerId.contains(cleanId)
        }
    }

    /**
     * Attempts to launch the native app directly, falling back to Web or Universal Media Search.
     */
    fun launchStreamingApp(
        context: Context,
        providerId: String?,
        movieTitle: String
    ): LaunchResult {
        val pm = context.packageManager

        // 1. If a specific provider ID is known, attempt to find its config
        val config = providerId?.let { findConfig(it) }
        val providerName = config?.displayName ?: (providerId?.replaceFirstChar { it.uppercase() } ?: "Streaming Service")

        if (config != null) {
            // Check installed packages
            for (pkg in config.packageNames) {
                try {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        launchIntent.putExtra(SearchManager.QUERY, movieTitle)
                        launchIntent.putExtra("query", movieTitle)
                        context.startActivity(launchIntent)
                        Log.d(TAG, "Successfully launched native app $pkg for $movieTitle")
                        return LaunchResult(true, providerName, "Opening ${config.displayName} for \"$movieTitle\"...")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed launching package $pkg: ${e.message}")
                }
            }

            // If not installed, try opening web search URL for that provider
            try {
                val webUrl = config.searchUrl(movieTitle)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                return LaunchResult(true, providerName, "Launching ${config.displayName} in browser for \"$movieTitle\"...")
            } catch (e: Exception) {
                Log.w(TAG, "Failed opening browser URL: ${e.message}")
            }
        }

        // 2. Generic fallback: Android Media Search Intent
        try {
            val mediaSearchIntent = Intent("android.media.action.MEDIA_SEARCH").apply {
                putExtra(SearchManager.QUERY, movieTitle)
                putExtra("query", movieTitle)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mediaSearchIntent.resolveActivity(pm) != null) {
                context.startActivity(mediaSearchIntent)
                return LaunchResult(true, providerName, "Broadcasting media search for \"$movieTitle\"...")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Media search intent failed: ${e.message}")
        }

        // 3. Fallback: Google search for streaming availability
        try {
            val searchUrl = "https://www.google.com/search?q=" + Uri.encode("watch $movieTitle online streaming")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
            return LaunchResult(true, providerName, "Finding streaming options for \"$movieTitle\"...")
        } catch (e: Exception) {
            Log.e(TAG, "All launch options failed: ${e.message}")
            return LaunchResult(false, providerName, "Could not open streaming app for \"$movieTitle\".")
        }
    }
}
