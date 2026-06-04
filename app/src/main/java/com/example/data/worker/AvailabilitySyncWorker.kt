package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.StreamApp
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.remote.OllamaChatMessage
import com.example.data.remote.OllamaChatRequest
import com.example.data.remote.OllamaClient
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
            // Zero-Touch Automation: Identify any watchlist items missing key research metadata or availability info
            val items = repository.allMediaItems.first()
            val pendingOrActiveItems = items.filter { 
                it.status == MediaStatus.PENDING_METADATA.name || 
                it.status == MediaStatus.WATCHLIST.name ||
                it.trivia.isNullOrEmpty() ||
                it.genres.isNullOrEmpty() ||
                it.imageUrl.isNullOrEmpty()
            }

            if (pendingOrActiveItems.isEmpty()) {
                Log.d(TAG, "No media items require sync at this time.")
                return Result.success()
            }

            Log.d(TAG, "Found ${pendingOrActiveItems.size} items to sync: ${pendingOrActiveItems.joinToString { it.title }}")

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

            // Fetch genre list once to map IDs to names
            val genreMap = try {
                val genreResponse = com.example.data.remote.TmdbClient.tmdbApiService.getGenreList(apiKey)
                genreResponse.genres.associate { it.id to it.name }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch genre list: ${e.message}")
                emptyMap<Int, String>()
            }

            // Fetch watch history for personalization
            val watchHistory = repository.getAllWatchSessionsList()
            val historySummary = if (watchHistory.isNotEmpty()) {
                watchHistory.take(15).joinToString(", ") { it.mediaItemTitle }
            } else {
                "No history yet"
            }

            val ollamaHost = userPrefs?.ollamaHost ?: "192.168.1.100"

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
                var syncedTrivia: String? = item.trivia
                var syncedGenres: String? = item.genres

                try {
                    Log.d(TAG, "Syncing metadata for: \"${item.title}\" (Current TMDB ID: $syncedTmdbId)")
                    // 1. Search for TMDB movie ID
                    val searchResponse = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(apiKey, item.title)
                    val match = searchResponse.results.firstOrNull()
                    if (match != null) {
                        val movieId = match.id
                        Log.d(TAG, "Found match for \"${item.title}\": ID $movieId, Genres: ${match.genreIds}")
                        syncedTmdbId = movieId.toString()
                        syncedOverview = match.overview ?: item.overview
                        syncedRating = match.voteAverage ?: item.rating
                        syncedPosterUrl = match.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: item.imageUrl

                        // 2. Fetch Providers for TMDB Movie ID
                        val providerResponse = com.example.data.remote.TmdbClient.tmdbApiService.getWatchProviders(movieId, apiKey)
                        val usCountry = providerResponse.results?.get("US")
                        val usProvidersList = mutableListOf<com.example.data.remote.TmdbProvider>()
                        usCountry?.flatrate?.let { usProvidersList.addAll(it) }
                        usCountry?.free?.let { usProvidersList.addAll(it) }
                        usCountry?.ads?.let { usProvidersList.addAll(it) }

                        Log.d(TAG, "TMDB Providers for \"${item.title}\": ${usProvidersList.joinToString { it.providerName }}")

                        if (usProvidersList.isNotEmpty()) {
                            syncedProviders = mapTmdbProvidersToLocal(usProvidersList)
                        } else {
                            syncedProviders = null
                        }
                        
                        Log.d(TAG, "Mapped Local Providers for \"${item.title}\": $syncedProviders")

                        // 2.1 Availability Notification Logic: Check if it's now available on an accessible service
                        // Trigger if newly available on any ACTIVE subscription OR any FREE service (Tubi, Freevee, etc.)
                        if (syncedProviders != null) {
                            val allProvidersList = repository.allStreamingProviders.first()
                            // Accessibile = Paid & Active OR Cost is 0.0
                            val accessibleProviders = allProvidersList.filter { it.isActive || it.costPerMonth == 0.0 }
                            val oldProviders = item.providersList.toSet()
                            val newProviders = syncedProviders.split(",").map { it.trim() }.toSet()
                            
                            val newlyAvailableOn = newProviders.filter { pId ->
                                !oldProviders.contains(pId) && accessibleProviders.any { it.id == pId }
                            }

                            if (newlyAvailableOn.isNotEmpty()) {
                                val firstProv = accessibleProviders.find { it.id == newlyAvailableOn.first() }
                                com.example.ui.NotificationHelper.showAvailabilityNotification(
                                    applicationContext,
                                    item.title,
                                    firstProv?.name ?: newlyAvailableOn.first()
                                )
                                Log.d(TAG, "Triggered availability notification for: \"${item.title}\" on ${firstProv?.name} (Free/Active check)")
                            }
                        }

                        // Extract genres from search result
                        syncedGenres = match.genreIds?.mapNotNull { genreMap[it] }?.joinToString(", ")

                        // 3. Synthesis Agent Research: Fetch Keywords, Cast, and Genres
                        try {
                            val keywordsResponse = com.example.data.remote.TmdbClient.tmdbApiService.getKeywords(movieId, apiKey)
                            val creditsResponse = com.example.data.remote.TmdbClient.tmdbApiService.getCredits(movieId, apiKey)
                            
                            val topKeywords = keywordsResponse.keywords.take(5).joinToString(", ") { it.name }
                            val topCast = creditsResponse.cast.take(3).joinToString(", ") { it.name }

                            // Synthesis 2.0: Use local Ollama (Gemma) for personalized research
                            val localSynthesis = generatePersonalizedSynthesis(
                                host = ollamaHost,
                                movieTitle = item.title,
                                overview = syncedOverview,
                                keywords = topKeywords,
                                cast = topCast,
                                history = historySummary
                            )

                            if (localSynthesis != null) {
                                syncedTrivia = localSynthesis
                                Log.d(TAG, "Local LLM Synthesis successful for \"${item.title}\"")
                            } else {
                                // Fallback to basic template if Ollama is offline
                                syncedTrivia = """
                                    ---
                                    focus_topics: "$topKeywords"
                                    featured_cast: "$topCast"
                                    agent_synthesis_date: "${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}"
                                    ---
                                    
                                    ### Why this belongs on your Watchlist:
                                    - Cultural Impact: This movie explores themes of $topKeywords.
                                    - Talent Profile: Features notable performances by $topCast.
                                    - Smart Sourcing: Cross-referenced with history summary: $historySummary.
                                """.trimIndent()
                                Log.d(TAG, "Ollama offline. Using basic template synthesis for \"${item.title}\"")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Synthesis Agent failed for \"${item.title}\": ${e.message}")
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
                    trivia = syncedTrivia,
                    genres = syncedGenres,
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

    private suspend fun generatePersonalizedSynthesis(
        host: String,
        movieTitle: String,
        overview: String?,
        keywords: String,
        cast: String,
        history: String
    ): String? {
        return try {
            val api = OllamaClient.getApiService(host)
            val prompt = """
                You are an advanced cinematic research agent called "Olivia". 
                Generate a structured research card for the movie: "$movieTitle".
                
                Context provided:
                - Overview: $overview
                - Keywords: $keywords
                - Cast: $cast
                - User's Watch History: $history
                
                Format your response EXACTLY as a Markdown YAML card like this:
                ---
                focus_topics: "[List 3-5 main themes]"
                featured_cast: "$cast"
                personal_relevance_score: "[Score 1-10 based on history]"
                ---
                
                ### Why this belongs on your Watchlist:
                - Cultural Impact: [Brief summary of themes]
                - Historical Connection: [Connect this movie to 1-2 titles from the user's watch history if possible]
                - Smart Sourcing: [Final recommendation punchline]
                
                Be concise, professional, and use a technical, "deep-wiki" tone.
            """.trimIndent()

            val request = OllamaChatRequest(
                messages = listOf(OllamaChatMessage(role = "user", content = prompt))
            )
            val response = api.chat(request)
            response.message.content
        } catch (e: Exception) {
            Log.e(TAG, "Ollama synthesis failed: ${e.message}")
            null
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
                name.contains("tubi") -> localIds.add("tubi")
                name.contains("freevee") -> localIds.add("freevee")
                name.contains("pluto") -> localIds.add("pluto")
            }
        }
        val result = localIds.distinct().joinToString(",")
        return if (result.isEmpty()) null else result
    }
}
