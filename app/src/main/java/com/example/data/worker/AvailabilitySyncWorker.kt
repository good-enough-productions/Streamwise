package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.StreamApp
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * WorkManager Background worker to sync streaming provider availability from TMDB API periodically.
 * Designed to run in background tasks, respecting system API rate limits and optimizing battery.
 */
class AvailabilitySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "AvailabilitySyncWorker"
        const val WORK_NAME = "com.example.data.worker.AvailabilitySyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic stream provider availability background sync...")

        // Retrieve repository from the Application Container
        val app = applicationContext as? StreamApp
        val repository = app?.container?.mediaRepository ?: return Result.failure()

        try {
            // Retrieve watchlist items that need metadata or availability updates
            val items = repository.allMediaItems.first()
            val pendingOrActiveItems = items.filter { 
                it.status == MediaStatus.PENDING_METADATA.name || it.status == MediaStatus.WATCHLIST.name 
            }

            if (pendingOrActiveItems.isEmpty()) {
                Log.d(TAG, "No media items require sync at this time.")
                return Result.success()
            }

            Log.d(TAG, "Found ${pendingOrActiveItems.size} items to sync.")

            // Prefer runtime-saved key; fall back to build-time key
            val userPrefs = app?.container?.userPreferences
            val runtimeKey = userPrefs?.tmdbApiKey ?: ""
            val buildTimeKey = com.example.BuildConfig.TMDB_API_KEY
            val apiKey = if (runtimeKey.isNotEmpty()) runtimeKey else buildTimeKey
            val isRealApiKeyConfigured = apiKey.isNotEmpty() && apiKey != "MY_TMDB_API_KEY"

            if (!isRealApiKeyConfigured) {
                Log.d(TAG, "No TMDB API key configured. Skipping sync — add your key in Settings.")
                return Result.success()
            }

            for (item in pendingOrActiveItems) {
                // Rate Limiting Optimization: Wait 1 second between calls to protect TMDB API rate-limit of 40 reqs/10s.
                delay(1000)

                if (isStopped) {
                    Log.d(TAG, "Sync work stopped by WorkManager constraint.")
                    return Result.retry()
                }

                var syncedProviders: String? = item.providerIds
                var syncedOverview: String? = item.overview
                var syncedRating: Double? = item.rating
                var syncedPosterUrl: String? = item.imageUrl
                var syncedTmdbId: String? = item.tmdbId

                try {
                    Log.d(TAG, "Fetching real metadata from TMDB for: ${item.title}")
                    // 1. Search for TMDB movie ID
                    val searchResponse = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(apiKey, item.title)
                    val match = searchResponse.results.firstOrNull()
                    if (match != null) {
                        syncedTmdbId = match.id.toString()
                        syncedOverview = match.overview ?: item.overview
                        syncedRating = match.voteAverage ?: item.rating
                        syncedPosterUrl = match.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: item.imageUrl

                        // 2. Fetch Providers for TMDB Movie ID
                        val providerResponse = com.example.data.remote.TmdbClient.tmdbApiService.getWatchProviders(match.id, apiKey)
                        val usProviders = providerResponse.results?.get("US")?.flatrate
                        if (usProviders != null) {
                            syncedProviders = mapTmdbProvidersToLocal(usProviders)
                        } else {
                            syncedProviders = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed calling TMDB API for \"${item.title}\": ${e.message}. Skipping this item.", e)
                    continue
                }

                // Update the Room database record with retrieved availability IDs and promote state
                val updatedItem = item.copy(
                    status = if (item.status == MediaStatus.PENDING_METADATA.name) MediaStatus.WATCHLIST.name else item.status,
                    providerIds = syncedProviders,
                    overview = syncedOverview,
                    rating = syncedRating,
                    imageUrl = syncedPosterUrl,
                    tmdbId = syncedTmdbId,
                    updatedAt = System.currentTimeMillis()
                )

                repository.updateMediaItem(updatedItem)
                Log.d(TAG, "Successfully synced availability for \"${item.title}\": $syncedProviders")
            }

            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in streaming availability sync: ${e.message}", e)
            return Result.retry()
        }
    }

    private fun mapTmdbProvidersToLocal(providers: List<com.example.data.remote.TmdbProvider>): String? {
        val localIds = mutableListOf<String>()
        for (provider in providers) {
            val name = provider.providerName.lowercase()
            when {
                name.contains("netflix") -> localIds.add("netflix")
                name.contains("hulu") -> localIds.add("hulu")
                name.contains("max") || name.contains("hbo") -> localIds.add("max")
                name.contains("disney") -> localIds.add("disney")
                name.contains("amazon") || name.contains("prime video") -> localIds.add("prime")
                name.contains("apple tv") -> localIds.add("apple")
            }
        }
        val result = localIds.distinct().joinToString(",")
        return if (result.isEmpty()) null else result
    }
}
