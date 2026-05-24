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

            for (item in pendingOrActiveItems) {
                // Rate Limiting Optimization: Wait 1 second between calls to protect TMDB API rate-limit of 40 reqs/10s.
                delay(1000)

                if (isStopped) {
                    Log.d(TAG, "Sync work stopped by WorkManager constraint.")
                    return Result.retry()
                }

                // Simulate/Mock Retrofit TMDB API call to fetch watch providers
                // In production, this would make network calls to:
                // 1. Search DB for TMDB ID: retrofittedTmdbApi.searchMulti(item.title)
                // 2. Fetch Providers: retrofittedTmdbApi.getWatchProviders(tmdbId, apiKey)
                // Map to local provider IDs (e.g. netflix, hulu, max, disney, etc.)
                
                val syncedProviders = simulateTmdbWatchProviderLookUp(item.title)
                val syncedOverview = if (item.overview.isNullOrEmpty()) {
                    "Discovered streaming option details for \"${item.title}\" via TMDB automatic background curation."
                } else {
                    item.overview
                }

                // Update the Room database record with retrieved availability IDs and promote state
                val updatedItem = item.copy(
                    status = if (item.status == MediaStatus.PENDING_METADATA.name) MediaStatus.WATCHLIST.name else item.status,
                    providerIds = syncedProviders,
                    overview = syncedOverview,
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

    /**
     * Simulates TMDB API watch-provider endpoint matching based on common movie catalogs.
     * In a full-network implementation, we call the TMDB Search API followed by /movie/{id}/watch/providers.
     */
    private fun simulateTmdbWatchProviderLookUp(title: String): String {
        val titleLower = title.lowercase()
        return when {
            titleLower.contains("white lotus") || titleLower.contains("hbo") || titleLower.contains("thrones") || titleLower.contains("wired") -> "max"
            titleLower.contains("stranger") || titleLower.contains("crown") || titleLower.contains("squid") || titleLower.contains("f1") -> "netflix"
            titleLower.contains("shōgun") || titleLower.contains("shogun") || titleLower.contains("bear") || titleLower.contains("under") -> "hulu,disney"
            titleLower.contains("mandalorian") || titleLower.contains("star wars") || titleLower.contains("marvel") -> "disney"
            titleLower.contains("boys") || titleLower.contains("rings") || titleLower.contains("reacher") -> "prime"
            titleLower.contains("lasso") || titleLower.contains("morning") || titleLower.contains("severance") -> "apple"
            else -> {
                // Return a mix of popular services randomly as a placeholder to show dynamic filters for custom additions
                val allPossibilities = listOf("netflix", "hulu", "max", "disney")
                allPossibilities.shuffled().take(2).joinToString(",")
            }
        }
    }
}
