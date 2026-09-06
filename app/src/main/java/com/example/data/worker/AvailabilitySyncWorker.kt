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
        const val ONE_TIME_WORK_NAME = "com.example.data.worker.AvailabilitySyncWorker.ONETIME"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic stream provider availability background sync...")

        // Retrieve repository from the Application Container
        val app = applicationContext as? StreamApp
        val repository = app?.container?.mediaRepository ?: return Result.failure()

        try {
            // Zero-Touch Automation: Identify watchlist items missing key research metadata or availability info
            val allItems = repository.allMediaItems.first()
            val watchlistItems = allItems.filter { 
                it.status == MediaStatus.PENDING_METADATA.name || it.status == MediaStatus.WATCHLIST.name 
            }
            val pendingOrActiveItems = if (watchlistItems.isNotEmpty()) {
                // Prioritize watchlist items that are missing providers or metadata (capped to 20 to respect WorkManager execution window)
                watchlistItems.sortedBy { if (it.providerIds.isNullOrEmpty() || it.trivia.isNullOrEmpty()) 0 else 1 }.take(20)
            } else {
                // If watchlist is up-to-date, backfill any items missing essential metadata
                allItems.filter { it.imageUrl.isNullOrEmpty() || it.genres.isNullOrEmpty() }.take(20)
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

            val ollamaHost = userPrefs?.ollamaHost ?: "192.168.86.217"
            // Fast reachability check to prevent 60-second socket timeouts when laptop is sleeping/off-network
            var isOllamaAvailable = OllamaClient.isHostReachable(ollamaHost, timeoutMs = 2000)
            if (!isOllamaAvailable) {
                Log.i(TAG, "Local Ollama host ($ollamaHost) is currently unreachable. Using offline template synthesis for this sync run.")
            } else {
                Log.i(TAG, "Local Ollama host ($ollamaHost) is reachable.")
            }

            val watchmodeKey = userPrefs?.watchmodeApiKey ?: ""
            val isWatchmodeConfigured = watchmodeKey.isNotEmpty()

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
                            // FALLBACK: If TMDB has no provider data, check Watchmode if configured
                            if (isWatchmodeConfigured) {
                                try {
                                    Log.d(TAG, "TMDB had no providers for \"${item.title}\". Querying Watchmode fallback...")
                                    val wmSearch = com.example.data.remote.WatchmodeClient.instance.searchTitle(watchmodeKey, searchValue = item.title)
                                    val wmMatch = wmSearch.results.firstOrNull()
                                    if (wmMatch != null) {
                                        val wmSources = com.example.data.remote.WatchmodeClient.instance.getTitleSources(wmMatch.id, watchmodeKey)
                                        if (wmSources.isNotEmpty()) {
                                            syncedProviders = mapWatchmodeProvidersToLocal(wmSources)
                                            Log.d(TAG, "Watchmode match found! Sources: ${wmSources.joinToString { it.name }} -> Mapped: $syncedProviders")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Watchmode fallback failed for \"${item.title}\": ${e.message}")
                                }
                            } else {
                                syncedProviders = "none"
                            }
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

                            // Synthesis 2.0: Use local Ollama (Gemma) for personalized research if host is reachable
                            val localSynthesis = if (isOllamaAvailable) {
                                val synth = generatePersonalizedSynthesis(
                                    host = ollamaHost,
                                    movieTitle = item.title,
                                    overview = syncedOverview,
                                    keywords = topKeywords,
                                    cast = topCast,
                                    history = historySummary
                                )
                                if (synth == null) {
                                    // Mark unavailable for subsequent items in this run if it failed
                                    isOllamaAvailable = false
                                }
                                synth
                            } else {
                                null
                            }

                            if (localSynthesis != null) {
                                syncedTrivia = localSynthesis
                                Log.d(TAG, "Local LLM Synthesis successful for \"${item.title}\"")
                            } else {
                                // Fallback to structured template if Ollama is offline
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
                                Log.d(TAG, "Ollama offline or synthesis skipped. Using template synthesis for \"${item.title}\"")
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
            Log.w(TAG, "Ollama synthesis call failed: ${e.message}. Falling back to template synthesis.")
            null
        }
    }

    private fun mapWatchmodeProvidersToLocal(sources: List<com.example.data.remote.WatchmodeSource>): String? {
        val localIds = mutableListOf<String>()
        for (source in sources) {
            val name = source.name.lowercase()
            // Watchmode type: "sub", "free", "rent", "buy"
            // Only care about streaming (sub/free)
            if (source.type != "sub" && source.type != "free") continue

            when {
                name.contains("netflix") -> localIds.add("netflix")
                name.contains("hulu") -> localIds.add("hulu")
                name.contains("max") || name.contains("hbo") -> localIds.add("max")
                name.contains("disney") -> localIds.add("disney")
                name.contains("amazon") || name.contains("prime video") -> localIds.add("prime")
                name.contains("apple tv") || name.contains("apple") -> localIds.add("apple")
                name.contains("criterion") -> localIds.add("criterion")
                name.contains("peacock") -> localIds.add("peacock")
                name.contains("paramount") -> localIds.add("paramount")
                name.contains("mubi") -> localIds.add("mubi")
                name.contains("shudder") -> localIds.add("shudder")
                name.contains("starz") -> localIds.add("starz")
                name.contains("amc") -> localIds.add("amc_plus")
                name.contains("britbox") -> localIds.add("britbox")
                name.contains("tubi") -> localIds.add("tubi")
                name.contains("freevee") -> localIds.add("freevee")
                name.contains("pluto") -> localIds.add("pluto")
                name.contains("kanopy") -> localIds.add("kanopy")
                name.contains("hoopla") -> localIds.add("hoopla")
            }
        }
        val result = localIds.distinct().joinToString(",")
        return result.ifEmpty { "none" }
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
                name.contains("apple tv") || name.contains("apple") -> localIds.add("apple")
                name.contains("criterion") -> localIds.add("criterion")
                name.contains("peacock") -> localIds.add("peacock")
                name.contains("paramount") -> localIds.add("paramount")
                name.contains("mubi") -> localIds.add("mubi")
                name.contains("shudder") -> localIds.add("shudder")
                name.contains("starz") -> localIds.add("starz")
                name.contains("amc") -> localIds.add("amc_plus")
                name.contains("britbox") -> localIds.add("britbox")
                name.contains("tubi") -> localIds.add("tubi")
                name.contains("freevee") -> localIds.add("freevee")
                name.contains("pluto") -> localIds.add("pluto")
                name.contains("kanopy") -> localIds.add("kanopy")
                name.contains("hoopla") -> localIds.add("hoopla")
            }
        }
        val result = localIds.distinct().joinToString(",")
        return result.ifEmpty { "none" }
    }
}
