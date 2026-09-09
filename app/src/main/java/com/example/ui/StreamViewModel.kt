package com.example.ui

import android.app.Application
import android.net.Uri
import org.json.JSONObject
import org.json.JSONArray
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.content.ContentResolver
import android.provider.OpenableColumns
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.UserPreferencesManager
import com.example.data.local.ProviderUsageStats
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.StreamingProvider
import com.example.data.model.GeminiAnalysisResult
import com.example.data.model.PodcastEpisode
import com.example.data.model.MovieNewsItem
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiGenerateRequest
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GitHubUpdateManager
import com.example.data.remote.UpdateStatus
import com.example.data.remote.LetterboxdSyncManager
import com.example.data.remote.LetterboxdSyncResult
import com.example.data.remote.LetterboxdFileImportResult
import com.example.data.repository.MediaRepository
import com.example.data.worker.AvailabilitySyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import com.example.data.util.CastMemberWithAge
import com.example.data.util.ActorAgeCalculator
import com.example.data.remote.TmdbPersonDetails
import com.example.data.remote.TmdbClient
import com.example.data.util.MediaTitleSanitizer
import java.util.concurrent.ConcurrentHashMap

class StreamViewModel(
    application: Application,
    private val repository: MediaRepository,
    private val userPreferences: UserPreferencesManager
) : AndroidViewModel(application) {

    init {
        com.example.data.model.PodcastEpisodeCatalog.initialize(application.applicationContext)
    }

    private val TAG = "StreamViewModel"

    // --- State Expositions ---

    // All registered media items
    // Watchlist items (PENDING_METADATA or WATCHLIST or INTENDING_TO_WATCH)
    val allMediaItems: StateFlow<List<MediaItem>> = repository.allMediaItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Watched history items (WATCHED)
    val watchedItems: StateFlow<List<MediaItem>> = repository.allMediaItems
        .map { items -> items.filter { it.status == MediaStatus.WATCHED.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All streaming providers
    val allProviders: StateFlow<List<StreamingProvider>> = repository.allStreamingProviders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current month's subscription usage and ROI insights
    val monthlyROIStats: StateFlow<List<ProviderUsageStats>> = repository.getCurrentMonthUsageStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State for managing foreground check-in flow targeting items marked "INTENDING_TO_WATCH"
    private val _activeCheckInItem = MutableStateFlow<MediaItem?>(null)
    val activeCheckInItem: StateFlow<MediaItem?> = _activeCheckInItem.asStateFlow()

    // Status UI messages or toast triggers
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Persisted TMDB API key (user-configurable at runtime)
    private val _tmdbApiKey = MutableStateFlow(userPreferences.tmdbApiKey)
    val tmdbApiKey: StateFlow<String> = _tmdbApiKey.asStateFlow()

    // Persisted Gemini API key (user-configurable at runtime, falls back to BuildConfig)
    private val _geminiApiKey = MutableStateFlow(
        userPreferences.geminiApiKey.ifEmpty {
            if (BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") BuildConfig.GEMINI_API_KEY else ""
        }
    )
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    // Persisted Watchmode API key
    private val _watchmodeApiKey = MutableStateFlow(userPreferences.watchmodeApiKey)
    val watchmodeApiKey: StateFlow<String> = _watchmodeApiKey.asStateFlow()

    // Persisted Ollama Host
    private val _ollamaHost = MutableStateFlow(userPreferences.ollamaHost)
    val ollamaHost: StateFlow<String> = _ollamaHost.asStateFlow()

    // Persisted GitHub Token
    private val _githubToken = MutableStateFlow(userPreferences.githubToken)
    val githubToken: StateFlow<String> = _githubToken.asStateFlow()

    // Beta Feedback FAB Setting
    private val _enableBetaFeedback = MutableStateFlow(userPreferences.enableBetaFeedback)
    val enableBetaFeedback: StateFlow<Boolean> = _enableBetaFeedback.asStateFlow()

    // Spotlight Collapsed Setting
    private val _isSpotlightCollapsed = MutableStateFlow(userPreferences.isSpotlightCollapsed)
    val isSpotlightCollapsed: StateFlow<Boolean> = _isSpotlightCollapsed.asStateFlow()

    fun toggleSpotlightCollapsed() {
        val newVal = !_isSpotlightCollapsed.value
        _isSpotlightCollapsed.value = newVal
        userPreferences.isSpotlightCollapsed = newVal
    }

    // View Mode (Grid vs List) Setting
    private val _isGridView = MutableStateFlow(userPreferences.isGridView)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun toggleGridView() {
        val newVal = !_isGridView.value
        _isGridView.value = newVal
        userPreferences.isGridView = newVal
    }

    // Google Sheet Webhook Setting
    private val _googleSheetWebhookUrl = MutableStateFlow(userPreferences.googleSheetWebhookUrl)
    val googleSheetWebhookUrl: StateFlow<String> = _googleSheetWebhookUrl.asStateFlow()

    fun saveGoogleSheetWebhookUrl(url: String) {
        val trimmed = url.trim()
        userPreferences.googleSheetWebhookUrl = trimmed
        _googleSheetWebhookUrl.value = trimmed
        _statusMessage.value = "Google Sheet webhook URL saved."
    }

    // Google Sheet Sync in Progress State
    private val _isSyncingToSheet = MutableStateFlow(false)
    val isSyncingToSheet: StateFlow<Boolean> = _isSyncingToSheet.asStateFlow()

    // Podcast Recommendations Sync State
    private val _isSyncingPodcasts = MutableStateFlow(false)
    val isSyncingPodcasts: StateFlow<Boolean> = _isSyncingPodcasts.asStateFlow()

    // Letterboxd Profile Settings
    private val _letterboxdUsername = MutableStateFlow(userPreferences.letterboxdUsername)
    val letterboxdUsername: StateFlow<String> = _letterboxdUsername.asStateFlow()

    fun saveLetterboxdUsername(username: String) {
        val clean = username.trim()
        userPreferences.letterboxdUsername = clean
        _letterboxdUsername.value = clean
        _statusMessage.value = "Letterboxd username updated."
    }

    // User Profile Display Name
    private val _userName = MutableStateFlow(userPreferences.userName)
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun saveUserName(name: String) {
        val clean = name.trim()
        userPreferences.userName = clean
        _userName.value = clean
        _statusMessage.value = "User name updated."
    }

    // Live Letterboxd RSS Sync
    private val _isSyncingLetterboxd = MutableStateFlow(false)
    val isSyncingLetterboxd: StateFlow<Boolean> = _isSyncingLetterboxd.asStateFlow()

    // Vault TMDB Rating Enrichment State
    private val _isEnrichingVault = MutableStateFlow(false)
    val isEnrichingVault: StateFlow<Boolean> = _isEnrichingVault.asStateFlow()

    private val _vaultEnrichProgress = MutableStateFlow("")
    val vaultEnrichProgress: StateFlow<String> = _vaultEnrichProgress.asStateFlow()

    private val _letterboxdSyncResult = MutableStateFlow<LetterboxdSyncResult?>(null)
    val letterboxdSyncResult: StateFlow<LetterboxdSyncResult?> = _letterboxdSyncResult.asStateFlow()

    fun clearLetterboxdSyncResult() {
        _letterboxdSyncResult.value = null
    }

    // Cast members with ages for currently inspected movie in MovieDetailsBottomSheet
    private val _selectedMovieCast = MutableStateFlow<List<CastMemberWithAge>>(emptyList())
    val selectedMovieCast: StateFlow<List<CastMemberWithAge>> = _selectedMovieCast.asStateFlow()

    private val _isLoadingCast = MutableStateFlow(false)
    val isLoadingCast: StateFlow<Boolean> = _isLoadingCast.asStateFlow()

    private val personCache = ConcurrentHashMap<Int, TmdbPersonDetails>()

    fun loadMovieCastWithAges(item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingCast.value = true
            _selectedMovieCast.value = emptyList()
            try {
                val apiKey = _tmdbApiKey.value.ifEmpty {
                    if (BuildConfig.TMDB_API_KEY != "MY_TMDB_API_KEY") BuildConfig.TMDB_API_KEY else ""
                }
                if (apiKey.isEmpty()) {
                    _isLoadingCast.value = false
                    return@launch
                }

                var movieId = item.tmdbId?.toIntOrNull()
                var releaseDate: String? = null

                if (movieId == null) {
                    val searchResp = TmdbClient.tmdbApiService.searchMovie(apiKey, item.title)
                    val best = searchResp.results.firstOrNull()
                    movieId = best?.id
                    releaseDate = best?.releaseDate
                }

                if (movieId == null) {
                    _isLoadingCast.value = false
                    return@launch
                }

                if (releaseDate.isNullOrBlank()) {
                    try {
                        val details = TmdbClient.tmdbApiService.getMovieDetails(movieId, apiKey)
                        releaseDate = details.releaseDate
                    } catch (e: Exception) {
                        val match = Regex("\\b(19\\d\\d|20\\d\\d)\\b").find(item.title)
                        releaseDate = match?.value
                    }
                }

                val credits = TmdbClient.tmdbApiService.getCredits(movieId, apiKey)
                val topCast = credits.cast.take(8)

                val castWithAges = topCast.map { castMember ->
                    async {
                        val person = if (castMember.id > 0) {
                            personCache.getOrPut(castMember.id) {
                                try {
                                    TmdbClient.tmdbApiService.getPersonDetails(castMember.id, apiKey)
                                } catch (e: Exception) {
                                    TmdbPersonDetails(
                                        id = castMember.id,
                                        name = castMember.name
                                    )
                                }
                            }
                        } else null

                        val age = ActorAgeCalculator.calculateAgeAtRelease(
                            birthday = person?.birthday,
                            releaseDate = releaseDate
                        )
                        val isDeceased = !person?.deathday.isNullOrBlank()
                        val deathAge = if (isDeceased) {
                            ActorAgeCalculator.calculateDeathAge(person?.birthday, person?.deathday)
                        } else null

                        CastMemberWithAge(
                            id = castMember.id,
                            name = castMember.name,
                            character = castMember.character,
                            profilePath = castMember.profilePath ?: person?.profilePath,
                            birthday = person?.birthday,
                            deathday = person?.deathday,
                            ageAtRelease = age,
                            isDeceased = isDeceased,
                            deathAge = deathAge
                        )
                    }
                }.awaitAll()

                _selectedMovieCast.value = castWithAges
            } catch (e: Exception) {
                Log.e(TAG, "Error loading movie cast with ages: ${e.message}", e)
            } finally {
                _isLoadingCast.value = false
            }
        }
    }

    init {
        // Automatically sync Letterboxd live diary in background on app startup
        val lbUser = userPreferences.letterboxdUsername.trim()
        if (lbUser.isNotBlank()) {
            syncLetterboxdLive(lbUser, silent = true)
        }
    }

    fun syncLetterboxdLive(username: String? = null, silent: Boolean = false) {
        val targetUser = (username ?: userPreferences.letterboxdUsername).trim()
        if (targetUser.isBlank()) {
            if (!silent) _statusMessage.value = "Please enter a Letterboxd username."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingLetterboxd.value = true
            if (!silent) _statusMessage.value = "Syncing diary from Letterboxd for @$targetUser..."
            val syncManager = LetterboxdSyncManager(repository.mediaDao)
            val result = syncManager.syncUserDiary(targetUser)
            _letterboxdSyncResult.value = result
            _isSyncingLetterboxd.value = false
            if (result.isSuccess) {
                userPreferences.lastLetterboxdSyncTime = System.currentTimeMillis()
                if (!silent) {
                    _statusMessage.value = "Letterboxd sync complete: ${result.newlyImportedCount} new titles imported!"
                }
            } else {
                if (!silent) {
                    _statusMessage.value = "Letterboxd sync: ${result.errorMessage}"
                }
            }
        }
    }

    // Letterboxd File (CSV/ZIP) Import
    private val _letterboxdFileImportResult = MutableStateFlow<LetterboxdFileImportResult?>(null)
    val letterboxdFileImportResult: StateFlow<LetterboxdFileImportResult?> = _letterboxdFileImportResult.asStateFlow()

    fun clearLetterboxdFileImportResult() {
        _letterboxdFileImportResult.value = null
    }

    fun importLetterboxdFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingLetterboxd.value = true
            _statusMessage.value = "Importing Letterboxd file..."
            try {
                var filename = "letterboxd_data.csv"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        filename = cursor.getString(nameIndex) ?: filename
                    }
                }

                contentResolver.openInputStream(uri)?.use { stream ->
                    val syncManager = LetterboxdSyncManager(repository.mediaDao)
                    val result = syncManager.importFileStream(stream, filename)
                    _letterboxdFileImportResult.value = result
                    if (result.isSuccess) {
                        val totalImported = result.watchlistImported + result.watchedImported
                        _statusMessage.value = "✓ Imported $totalImported titles from $filename (${result.watchlistImported} watchlist, ${result.watchedImported} watched)"
                        userPreferences.lastLetterboxdSyncTime = System.currentTimeMillis()
                    } else {
                        _statusMessage.value = "Import error: ${result.errorMessage ?: "Unknown error"}"
                    }
                } ?: run {
                    _statusMessage.value = "Could not open selected file."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing Letterboxd file: ${e.message}", e)
                _statusMessage.value = "Error: ${e.localizedMessage ?: "Failed to read file"}"
            } finally {
                _isSyncingLetterboxd.value = false
            }
        }
    }

    /**
     * Generates a Letterboxd-compatible CSV string (Title,Year,Rating10,WatchedDate)
     * for all watched items logged in Streamwise.
     */
    suspend fun exportLetterboxdCsv(): String = withContext(Dispatchers.IO) {
        val watched = repository.allMediaItems.first().filter { it.status == MediaStatus.WATCHED.name }
        val syncManager = LetterboxdSyncManager(repository.mediaDao)
        syncManager.generateLetterboxdExportCsv(watched)
    }

    // Update Streaming Provider (cost, plan name, active status, tenure)
    fun updateStreamingProvider(provider: StreamingProvider) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStreamingProvider(provider)
            _statusMessage.value = "${provider.name} updated."
        }
    }

    // GitHub OTA Auto-Updater
    val gitHubUpdateManager = GitHubUpdateManager(getApplication())
    val updateStatus: StateFlow<UpdateStatus> = gitHubUpdateManager.updateStatus

    fun checkForUpdates() {
        viewModelScope.launch {
            gitHubUpdateManager.checkForUpdates(BuildConfig.VERSION_NAME)
        }
    }

    fun downloadAndInstallUpdate(downloadUrl: String) {
        viewModelScope.launch {
            gitHubUpdateManager.downloadAndInstallApk(downloadUrl)
        }
    }

    fun resetUpdateStatus() {
        gitHubUpdateManager.resetStatus()
    }


    /**
     * 1-Tap Letterboxd Sync: Sends Watched Vault and Watchlist data directly to the Google Sheet backend.
     */
    fun syncLetterboxdToGoogleSheet() {
        viewModelScope.launch(Dispatchers.IO) {
            val webhookUrl = userPreferences.googleSheetWebhookUrl
            if (webhookUrl.isBlank()) {
                _statusMessage.value = "Please configure Google Sheet Webhook URL in Settings first."
                return@launch
            }

            _isSyncingToSheet.value = true
            _statusMessage.value = "Syncing Letterboxd archive to Google Sheet backend..."
            try {
                val allItems = repository.allMediaItems.first()
                val watched = allItems.filter { it.status == MediaStatus.WATCHED.name }
                val watchlist = allItems.filter { it.status != MediaStatus.WATCHED.name }

                val watchedArray = JSONArray()
                watched.take(600).forEach { item ->
                    val obj = JSONObject().apply {
                        put("title", item.title)
                        put("rating", item.rating ?: JSONObject.NULL)
                        put("watchedAt", item.watchedAt ?: item.addedAt)
                        put("genres", item.genres ?: "")
                        put("notes", item.userNotes ?: "")
                        put("providers", (item.providerIds ?: "").let { if (it == "none") "" else it })
                    }
                    watchedArray.put(obj)
                }

                val watchlistArray = JSONArray()
                watchlist.take(300).forEach { item ->
                    val obj = JSONObject().apply {
                        put("title", item.title)
                        put("rating", item.rating ?: JSONObject.NULL)
                        put("addedAt", item.addedAt)
                        put("genres", item.genres ?: "")
                        put("providers", (item.providerIds ?: "").let { if (it == "none") "" else it })
                    }
                    watchlistArray.put(obj)
                }

                val payload = JSONObject().apply {
                    put("action", "syncLetterboxd")
                    put("app", "Streamwise")
                    put("timestamp", System.currentTimeMillis())
                    put("totalWatchedCount", watched.size)
                    put("totalWatchlistCount", watchlist.size)
                    put("watched", watchedArray)
                    put("watchlist", watchlistArray)
                }

                var currentUrl = webhookUrl
                var conn: HttpURLConnection? = null
                var redirects = 0
                var code = 0

                // Follow potential HTTP 302 redirects commonly returned by Google Apps Script web apps
                while (redirects < 5) {
                    val url = URL(currentUrl)
                    conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 15000
                        readTimeout = 25000
                        instanceFollowRedirects = false
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        setRequestProperty("Accept", "application/json")
                    }
                    conn.outputStream.use { os ->
                        os.write(payload.toString().toByteArray(Charsets.UTF_8))
                    }
                    code = conn.responseCode
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307 || code == 308) {
                        val newLocation = conn.getHeaderField("Location")
                        if (!newLocation.isNullOrBlank()) {
                            currentUrl = newLocation
                            redirects++
                            conn.disconnect()
                            continue
                        }
                    }
                    break
                }

                if (code in 200..302) {
                    _statusMessage.value = "✓ Synced ${watched.size} watched & ${watchlist.size} watchlist titles to Google Sheet!"
                } else {
                    _statusMessage.value = "Google Sheet sync finished with code $code"
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to sync to Google Sheet", e)
                _statusMessage.value = "Sync error: ${e.localizedMessage ?: "Network failed"}"
            } finally {
                _isSyncingToSheet.value = false
            }
        }
    }

    private val podcastSyncMutex = Mutex()

    suspend fun syncPodcastRecommendationsInternal(): Int = podcastSyncMutex.withLock {
        val webhookUrl = userPreferences.googleSheetWebhookUrl
        var currentUrl = if (webhookUrl.contains("?")) "$webhookUrl&action=getPodcastRecs" else "$webhookUrl?action=getPodcastRecs"

        var conn: HttpURLConnection? = null
        var redirects = 0
        var code = 0

        while (redirects < 5) {
            val url = URL(currentUrl)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 25000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
            }
            code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307 || code == 308) {
                val newLocation = conn.getHeaderField("Location")
                if (!newLocation.isNullOrBlank()) {
                    currentUrl = newLocation
                    redirects++
                    conn.disconnect()
                    continue
                }
            }
            break
        }

        if (code in 200..299 && conn != null) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            if (json.optBoolean("success", false)) {
                val recs = json.optJSONArray("recommendations") ?: JSONArray()
                val allItems = repository.mediaDao.getAllMediaItemsList()
                val existingMap = allItems.associateBy { it.title.lowercase().trim() }.toMutableMap()

                val runtimeKey = userPreferences.tmdbApiKey
                val buildTimeKey = com.example.BuildConfig.TMDB_API_KEY
                val key = if (runtimeKey.isNotEmpty() && runtimeKey != "MY_TMDB_API_KEY") runtimeKey else buildTimeKey

                val genreMap = if (key.isNotEmpty() && key != "MY_TMDB_API_KEY") {
                    try {
                        com.example.data.remote.TmdbClient.tmdbApiService.getGenreList(key).genres.associate { it.id to it.name }
                    } catch (e: Exception) { emptyMap() }
                } else emptyMap()

                var addedCount = 0
                var updatedCount = 0

                for (i in 0 until recs.length()) {
                    val obj = recs.getJSONObject(i)
                    val rawTitle = obj.optString("title").trim()
                    if (rawTitle.isBlank()) continue

                    // 1. Strict non-movie rejection
                    if (MediaTitleSanitizer.isNonMovieEpisode(rawTitle)) continue

                    val cleanTitle = MediaTitleSanitizer.cleanCandidateTitle(rawTitle)
                    if (cleanTitle.isBlank() || MediaTitleSanitizer.isNonMovieEpisode(cleanTitle)) continue

                    val podcast = obj.optString("podcast")
                    val episode = obj.optString("episode")
                    val airDate = obj.optString("airDate")
                    val by = obj.optString("recommendedBy")
                    val verdict = obj.optString("verdict")
                    val context = obj.optString("context")

                    val sourceTag = if (podcast.isNotBlank()) "Podcast: $podcast" else "Podcast Rec"
                    val tagNotes = buildString {
                        if (podcast.isNotBlank()) append("[$podcast] ")
                        if (episode.isNotBlank()) append(episode)
                        if (verdict.isNotBlank()) append(" ($verdict)")
                    }.trim()

                    val normKey = cleanTitle.lowercase().trim()
                    val existing = existingMap[normKey] ?: existingMap[rawTitle.lowercase().trim()]
                    if (existing != null) {
                        val currentNotes = existing.userNotes ?: ""
                        if (!currentNotes.contains(podcast) && podcast.isNotBlank()) {
                            val newNotes = if (currentNotes.isBlank()) tagNotes else "$currentNotes • $tagNotes"
                            val updated = existing.copy(
                                importSource = existing.importSource ?: sourceTag,
                                userNotes = newNotes.take(300),
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.updateMediaItem(updated)
                            existingMap[normKey] = updated
                            updatedCount++
                        }
                    } else {
                        // Gating requirement: Must be verified on TMDB before adding as a movie
                        if (key.isNotEmpty() && key != "MY_TMDB_API_KEY") {
                            var match: com.example.data.remote.TmdbSearchResult? = null
                            try {
                                val searchResp = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(key, cleanTitle)
                                match = searchResp.results.firstOrNull()
                                if (match == null && cleanTitle.contains(":")) {
                                    val prefix = cleanTitle.substringBefore(":").trim()
                                    if (prefix.length >= 3) {
                                        match = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(key, prefix).results.firstOrNull()
                                    }
                                }
                            } catch (e: Exception) { null }

                            if (match != null) {
                                val tmdbId = match.id.toString()
                                val poster = match.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                                val genres = match.genreIds?.mapNotNull { genreMap[it] }?.joinToString(", ")
                                val releaseDate = match.releaseDate

                                var providersString: String? = null
                                try {
                                    val provResp = com.example.data.remote.TmdbClient.tmdbApiService.getWatchProviders(match.id, key)
                                    val us = provResp.results?.get("US")
                                    val list = mutableListOf<com.example.data.remote.TmdbProvider>()
                                    us?.flatrate?.let { list.addAll(it) }
                                    us?.free?.let { list.addAll(it) }
                                    us?.ads?.let { list.addAll(it) }
                                    providersString = mapTmdbProviders(list)
                                } catch (e: Exception) { }

                                val newItem = MediaItem(
                                    title = match.title,
                                    status = MediaStatus.WATCHLIST.name,
                                    tmdbId = tmdbId,
                                    imageUrl = poster,
                                    rating = match.voteAverage,
                                    overview = match.overview,
                                    genres = genres,
                                    releaseDate = releaseDate,
                                    providerIds = providersString ?: "none",
                                    importSource = sourceTag,
                                    userNotes = tagNotes,
                                    addedAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                                val newId = repository.insertMediaItem(newItem)
                                existingMap[match.title.lowercase().trim()] = newItem.copy(id = newId)
                                existingMap[normKey] = newItem.copy(id = newId)
                                addedCount++
                            }
                            // If match == null, DISCARD! Never insert unmatched podcast episodes!
                        }
                    }
                }

                _statusMessage.value = "✓ Synced ${recs.length()} podcast recs: +$addedCount Watchlist, ~$updatedCount Vault updated!"
                return@withLock addedCount
            } else {
                _statusMessage.value = "Podcast sync error: ${json.optString("error", "Unknown error")}"
            }
        } else {
            _statusMessage.value = "Podcast sync returned HTTP $code"
        }
        return@withLock 0
    }

    suspend fun scrubAndEnrichUnmatchedTitles(): Pair<Int, Int> {
        val runtimeKey = userPreferences.tmdbApiKey
        val buildTimeKey = com.example.BuildConfig.TMDB_API_KEY
        val key = if (runtimeKey.isNotEmpty() && runtimeKey != "MY_TMDB_API_KEY") runtimeKey else buildTimeKey

        val allItems = repository.mediaDao.getAllMediaItemsList()
        val watchlistItems = allItems.filter { it.status == MediaStatus.WATCHLIST.name }

        val genreMap = if (key.isNotEmpty() && key != "MY_TMDB_API_KEY") {
            try {
                com.example.data.remote.TmdbClient.tmdbApiService.getGenreList(key).genres.associate { it.id to it.name }
            } catch (e: Exception) { emptyMap() }
        } else emptyMap()

        val itemsToDelete = mutableListOf<MediaItem>()
        var enrichedCount = 0

        for (item in watchlistItems) {
            // 1. Immediately delete any title that is a known non-movie episode pattern
            if (MediaTitleSanitizer.isNonMovieEpisode(item.title)) {
                itemsToDelete.add(item)
                continue
            }

            // 2. If item has no TMDB ID or image:
            if (item.tmdbId.isNullOrEmpty() || item.imageUrl.isNullOrEmpty()) {
                val cleaned = MediaTitleSanitizer.cleanCandidateTitle(item.title)
                if (MediaTitleSanitizer.isNonMovieEpisode(cleaned)) {
                    itemsToDelete.add(item)
                    continue
                }

                if (key.isNotEmpty() && key != "MY_TMDB_API_KEY") {
                    var match: com.example.data.remote.TmdbSearchResult? = null
                    try {
                        val searchResp = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(key, cleaned)
                        match = searchResp.results.firstOrNull()
                        if (match == null && cleaned.contains(":")) {
                            val prefix = cleaned.substringBefore(":").trim()
                            if (prefix.length >= 3) {
                                match = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(key, prefix).results.firstOrNull()
                            }
                        }
                    } catch (e: Exception) { null }

                    if (match != null) {
                        val poster = match.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                        val genres = match.genreIds?.mapNotNull { genreMap[it] }?.joinToString(", ") ?: item.genres
                        val updated = item.copy(
                            title = match.title,
                            tmdbId = match.id.toString(),
                            imageUrl = poster ?: item.imageUrl,
                            releaseDate = match.releaseDate ?: item.releaseDate,
                            rating = match.voteAverage ?: item.rating,
                            overview = match.overview?.ifBlank { item.overview } ?: item.overview,
                            genres = genres,
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.updateMediaItem(updated)
                        enrichedCount++
                    } else {
                        // Could not match to TMDB and has no metadata -> delete from watchlist
                        itemsToDelete.add(item)
                    }
                }
            }
        }

        if (itemsToDelete.isNotEmpty()) {
            itemsToDelete.map { it.id }.chunked(500).forEach { chunk ->
                repository.deleteMediaItemsByIdList(chunk)
            }
        }

        if (itemsToDelete.isNotEmpty() || enrichedCount > 0) {
            android.util.Log.d(TAG, "Data integrity scrub: Purged ${itemsToDelete.size} non-movies, enriched $enrichedCount titles.")
        }
        return Pair(itemsToDelete.size, enrichedCount)
    }

    fun syncPodcastRecommendations() {
        if (_isSyncingPodcasts.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncingPodcasts.value = true
            try {
                _statusMessage.value = "Fetching Gemini Spark podcast recommendations..."
                val added = syncPodcastRecommendationsInternal()
                if (added > 0) {
                    repository.deduplicateMediaItems()
                    syncWatchlistMetadata(forceAll = false)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Podcast recommendations sync failed", e)
                _statusMessage.value = "Podcast sync failed: ${e.localizedMessage ?: "Network error"}"
            } finally {
                _isSyncingPodcasts.value = false
            }
        }
    }

    init {
        // Run database deduplication on startup to eliminate any duplicate entries
        viewModelScope.launch(Dispatchers.IO) {
            val removed = repository.deduplicateMediaItems()
            if (removed > 0) {
                android.util.Log.d(TAG, "Deduplicated $removed duplicate media items on startup.")
            }
            // Scrub and enrich unmatched non-movie podcast episodes
            try {
                val (purged, enriched) = scrubAndEnrichUnmatchedTitles()
                if (purged > 0 || enriched > 0) {
                    android.util.Log.d(TAG, "Startup integrity check: -${purged} non-movies, +${enriched} enriched.")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Startup scrub failed: ${e.message}")
            }
            // Auto-sync Gemini Spark podcast recommendations on startup
            try {
                val added = syncPodcastRecommendationsInternal()
                if (added > 0) {
                    repository.deduplicateMediaItems()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Startup podcast sync failed: ${e.message}")
            }
            // Auto-sync TMDB metadata on launch if items are unpopulated
            kotlinx.coroutines.delay(500)
            syncWatchlistMetadata(forceAll = false)
        }
    }

    fun setEnableBetaFeedback(enabled: Boolean) {
        userPreferences.enableBetaFeedback = enabled
        _enableBetaFeedback.value = enabled
        _statusMessage.value = if (enabled) "Beta feedback button enabled" else "Beta feedback button hidden"
    }

    fun saveGeminiApiKey(key: String) {
        userPreferences.geminiApiKey = key
        _geminiApiKey.value = key.trim()
        _statusMessage.value = if (key.isBlank()) "Gemini API key cleared." else "Gemini API key saved."
        if (key.isNotBlank()) {
            runGeminiProAnalysis()
        }
    }

    fun saveTmdbApiKey(key: String) {
        userPreferences.tmdbApiKey = key
        _tmdbApiKey.value = key.trim()
        _statusMessage.value = if (key.isBlank()) "TMDB API key cleared." else "TMDB API key saved."
        if (key.isNotBlank()) {
            syncWatchlistMetadata(forceAll = false)
        }
    }

    fun saveWatchmodeApiKey(key: String) {
        userPreferences.watchmodeApiKey = key
        _watchmodeApiKey.value = key.trim()
        _statusMessage.value = if (key.isBlank()) "Watchmode API key cleared." else "Watchmode API key saved."
    }

    fun saveOllamaHost(host: String) {
        userPreferences.ollamaHost = host
        _ollamaHost.value = host.trim()
        _statusMessage.value = "Local Ollama host updated: $host"
    }

    fun saveGithubToken(token: String) {
        userPreferences.githubToken = token
        _githubToken.value = token.trim()
        _statusMessage.value = "GitHub token saved for Self-Evolving workflows."
    }

    // Theme Management (Supports immediate Dark / Light mode switching)
    private val _isDarkMode = MutableStateFlow(userPreferences.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        userPreferences.isDarkMode = next
        _isDarkMode.value = next
    }

    // Explore: Gemini Pro Custom Analysis & Curated Content
    private val _geminiAnalysis = MutableStateFlow<GeminiAnalysisResult?>(null)
    val geminiAnalysis: StateFlow<GeminiAnalysisResult?> = _geminiAnalysis.asStateFlow()

    private val _isAnalyzingWithGemini = MutableStateFlow(false)
    val isAnalyzingWithGemini: StateFlow<Boolean> = _isAnalyzingWithGemini.asStateFlow()

    // Curated Podcasts linked to Cinephile Vault
    val podcastEpisodes: List<PodcastEpisode> = listOf(
        PodcastEpisode(
            id = "pod_1",
            showTitle = "The Big Picture",
            episodeTitle = "The Modern Sci-Fi Renaissance: Denis Villeneuve & Beyond",
            duration = "1h 14m",
            description = "Sean Fennessey and Amanda Dobbins break down the resurgence of big-canvas auteur cinema and thematic ambition in modern filmmaking.",
            date = "Recent Release",
            podcastUrl = "https://open.spotify.com/search/The%20Big%20Picture%20Denis%20Villeneuve"
        ),
        PodcastEpisode(
            id = "pod_2",
            showTitle = "The Rewatchables",
            episodeTitle = "Michael Mann's 'Heat': The Masterpiece Breakdown",
            duration = "1h 48m",
            description = "Bill Simmons, Chris Ryan, and Andy Greenwald dive into Mann's definitive LA crime saga, Pacino vs. De Niro, and untouchable cinematography.",
            date = "Classic Deep Dive",
            podcastUrl = "https://open.spotify.com/search/The%20Rewatchables%20Heat%20Michael%20Mann"
        ),
        PodcastEpisode(
            id = "pod_3",
            showTitle = "Blank Check with Griffin & David",
            episodeTitle = "Christopher Nolan: The Non-Linear Epic & 65mm IMAX Craft",
            duration = "2h 18m",
            description = "A deep examination of practical IMAX craft, non-linear narrative architecture, and intense editing rhythms from Memento to Oppenheimer.",
            date = "Director Retrospective",
            podcastUrl = "https://open.spotify.com/search/Blank%20Check%20Christopher%20Nolan"
        ),
        PodcastEpisode(
            id = "pod_4",
            showTitle = "Filmspotting",
            episodeTitle = "Criterion & MUBI: Essential Restorations & Hidden Gems",
            duration = "1h 05m",
            description = "Adam Kempenaar and Josh Larsen highlight overlooked psychological thrillers and foreign language classics worth prioritizing on boutique services.",
            date = "Curation Special",
            podcastUrl = "https://open.spotify.com/search/Filmspotting%20Criterion%20MUBI"
        )
    )

    // Curated Movie News & Industry Trends
    val movieNews: List<MovieNewsItem> = listOf(
        MovieNewsItem(
            id = "news_1",
            title = "Auteur Renaissance: 4K Restorations Announced for 70s Neo-Noirs",
            category = "Restorations",
            summary = "Janus Films and The Criterion Collection unveil new 4K digital transfers with original magnetic audio stems for seminal American New Wave thrillers.",
            source = "Criterion Daily",
            date = "Today"
        ),
        MovieNewsItem(
            id = "news_2",
            title = "Cannes & Venice Festival Laureates Arriving on Premium Streaming",
            category = "Festivals",
            summary = "Boutique curators Neon and MUBI secure exclusive windowing rights for festival standouts ahead of the upcoming awards season.",
            source = "IndieWire",
            date = "Yesterday"
        ),
        MovieNewsItem(
            id = "news_3",
            title = "Physical Media & High-Bitrate Streaming Surge Among Cinephiles",
            category = "Industry",
            summary = "New telemetry reveals viewers watching dense cinematography are migrating toward dedicated high-bitrate streaming and offline local vaults.",
            source = "Variety",
            date = "This Week"
        )
    )

    init {
        runGeminiProAnalysis()
    }

    fun runGeminiProAnalysis() {
        viewModelScope.launch(Dispatchers.IO) {
            _isAnalyzingWithGemini.value = true
            try {
                val watched = watchedItems.value
                val sampleWatched = watched.take(25).map { item ->
                    val genrePart = if (!item.genres.isNullOrEmpty()) " [${item.genres}]" else ""
                    "${item.title}$genrePart"
                }
                val watchlist = allMediaItems.value.filter { it.status == MediaStatus.WATCHLIST.name }.take(15).map { it.title }
                
                // Real-time top genres from the user's watched vault
                val topGenres = watched.mapNotNull { it.genres }
                    .flatMap { it.split(",", "/").map { g -> g.trim() } }
                    .filter { it.isNotEmpty() }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(6)
                    .joinToString { "${it.key} (${it.value})" }

                val userContext = buildString {
                    append("Total watched movies in cinephile vault: ${watched.size}\n")
                    if (topGenres.isNotEmpty()) {
                        append("Dominant vault genres: $topGenres\n")
                    }
                    append("Sample recent titles: ")
                    append(if (sampleWatched.isNotEmpty()) sampleWatched.joinToString(", ") else "Inception, Heat, Blade Runner 2049, Arrival, Oppenheimer")
                    append("\nCurrent watchlist queue: ")
                    append(if (watchlist.isNotEmpty()) watchlist.joinToString(", ") else "Dune: Part Two, Chinatown, Memories of Murder, The Master")
                }

                val prompt = """
                    You are a world-class film scholar and cinema curator analyzing a cinephile's personal movie vault.
                    
                    $userContext
                    
                    Please provide an insightful, highly engaging cinema analysis in valid JSON format with these exact keys:
                    {
                      "headline": "A bold, stylish 3-6 word theme headline capturing their taste profile",
                      "narrative": "A rich 2-3 sentence paragraph analyzing the recurring cinematic aesthetics, themes, existential questions, or visual styles across their titles.",
                      "themes": ["Theme 1", "Theme 2", "Theme 3", "Theme 4"],
                      "auteurConnections": ["Connection/Director 1", "Connection/Director 2"],
                      "recommendations": [
                        {"title": "Film Title 1", "reason": "Specific 1-sentence reason why it connects to their vault"},
                        {"title": "Film Title 2", "reason": "Specific 1-sentence reason why it connects to their vault"},
                        {"title": "Film Title 3", "reason": "Specific 1-sentence reason why it connects to their vault"}
                      ]
                    }
                    Respond ONLY with the JSON object.
                """.trimIndent()

                val apiKey = _geminiApiKey.value.ifEmpty { BuildConfig.GEMINI_API_KEY }
                var analysisResult: GeminiAnalysisResult? = null

                if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
                    try {
                        val request = GeminiGenerateRequest(
                            contents = listOf(
                                GeminiContent(parts = listOf(GeminiPart(text = prompt)))
                            ),
                            generationConfig = GeminiGenerationConfig(temperature = 0.4f)
                        )
                        val response = GeminiClient.apiService.generateContent(apiKey, request)
                        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!text.isNullOrBlank()) {
                            val cleanJson = text.trim()
                                .removePrefix("```json")
                                .removePrefix("```")
                                .removeSuffix("```")
                                .trim()
                            val json = JSONObject(cleanJson)
                            val headline = json.optString("headline", "Atmospheric Neo-Noir & High-Concept Precision")
                            val narrative = json.optString("narrative", "Your vault reveals a distinct gravitation toward atmospheric tension, morally ambiguous protagonists, and meticulous director-driven visual storytelling.")
                            
                            val themesList = mutableListOf<String>()
                            json.optJSONArray("themes")?.let { arr ->
                                for (i in 0 until arr.length()) themesList.add(arr.getString(i))
                            }
                            if (themesList.isEmpty()) {
                                themesList.addAll(listOf("Neo-Noir Atmosphere", "Existential Sci-Fi", "Moral Ambiguity"))
                            }

                            val auteursList = mutableListOf<String>()
                            json.optJSONArray("auteurConnections")?.let { arr ->
                                for (i in 0 until arr.length()) auteursList.add(arr.getString(i))
                            }
                            if (auteursList.isEmpty()) {
                                auteursList.addAll(listOf("Denis Villeneuve ↔ Christopher Nolan", "Michael Mann ↔ David Fincher"))
                            }

                            val recsList = mutableListOf<Pair<String, String>>()
                            json.optJSONArray("recommendations")?.let { arr ->
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    recsList.add(obj.optString("title") to obj.optString("reason"))
                                }
                            }

                            analysisResult = GeminiAnalysisResult(
                                headline = headline,
                                narrative = narrative,
                                themes = themesList,
                                auteurConnections = auteursList,
                                recommendations = recsList
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Gemini API call error, applying intelligent vault synthesis", e)
                    }
                }

                if (analysisResult == null) {
                    analysisResult = synthesizeVaultAnalysis(watched.map { it.title }, watchlist)
                }

                _geminiAnalysis.value = analysisResult
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error running Gemini analysis", e)
            } finally {
                _isAnalyzingWithGemini.value = false
            }
        }
    }

    private fun synthesizeVaultAnalysis(watched: List<String>, watchlist: List<String>): GeminiAnalysisResult {
        val sampleTitles = (watched + watchlist).take(4)
        val titleHighlight = if (sampleTitles.isNotEmpty()) sampleTitles.joinToString(", ") else "Heat, Inception, Blade Runner 2049"
        return GeminiAnalysisResult(
            headline = "Atmospheric Neo-Noir & High-Concept Precision",
            narrative = "Curating across titles like $titleHighlight, your cinematic vault highlights a distinct taste for deliberate pacing, high-stakes moral conflict, and world-building directed by visionary auteurs.",
            themes = listOf("Psychological Thriller", "Auteur Sci-Fi", "Cerebral Crime", "65mm/IMAX Aesthetics"),
            auteurConnections = listOf(
                "Michael Mann ↔ Denis Villeneuve (Tactile, atmospheric realism)",
                "Christopher Nolan ↔ David Fincher (Obsessive structural precision)"
            ),
            recommendations = listOf(
                "Memories of Murder (2003)" to "Masterful investigative tension mirroring your affinity for atmospheric procedural drama.",
                "Thief (1981)" to "The pinnacle of existential neon-lit noir that directly influenced your modern crime favorites.",
                "Solaris (1972)" to "Slow-burn philosophical science fiction expanding on the existential themes in your watchlist."
            )
        )
    }

    /**
     * 1-Tap Watchlist addition directly from Gemini / Explore recommendations.
     * Inserts the title under PENDING_METADATA and immediately triggers TMDB metadata enrichment.
     */
    fun addRecommendationToWatchlist(title: String, reason: String = "") {
        viewModelScope.launch {
            val cleanTitle = title.replace(Regex("\\s*\\(\\d{4}\\)$"), "").trim()
            val item = MediaItem(
                title = cleanTitle,
                userNotes = if (reason.isNotBlank()) "Gemini Recommendation: $reason" else "Recommended by Gemini Pro",
                importSource = "Gemini Pro Explore",
                status = MediaStatus.PENDING_METADATA.name
            )
            repository.insertMediaItem(item)
            _statusMessage.value = "Added \"$cleanTitle\" to your Watchlist!"
            syncWatchlistMetadata(forceAll = false)
        }
    }

    // Casting State
    val discoveredDevices: StateFlow<List<CastDevice>> = CastingManager.discoveredDevices

    // Chatbot State
    private val _chatMessages = MutableStateFlow<List<com.example.data.remote.OllamaChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<com.example.data.remote.OllamaChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendChatMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isChatLoading.value = true
            
            // Append user message
            val currentChat = _chatMessages.value.toMutableList()
            currentChat.add(com.example.data.remote.OllamaChatMessage("user", userMessage))
            _chatMessages.value = currentChat

            try {
                // Fetch context
                val watchHistory = watchedItems.value.take(20).joinToString(", ") { it.title }
                val watchlist = allMediaItems.value.filter { it.status == MediaStatus.WATCHLIST.name }.take(20).joinToString(", ") { it.title }

                val systemPrompt = """
                    You are Olivia, an advanced cinematic research agent and app co-developer.
                    
                    USER DATA:
                    - History: ${watchHistory}
                    - Watchlist: ${watchlist}
                    
                    AGENTIC PROTOCOL:
                    1. BE CONVERSATIONAL: Answer questions about movies, give recommendations, and analyze trends.
                    2. PRECISE FEEDBACK: Only if the user explicitly asks for an app change, reports a bug, or suggests a specific new feature, you must trigger a GitHub Issue.
                    3. TRIGGER FORMAT: To trigger an issue, your response MUST conclude with exactly this block:
                       <github_issue>
                       {
                         "title": "[FEATURE/BUG]: Brief Title",
                         "body": "Clear description of the requested change for the Android codebase."
                       }
                       </github_issue>
                       
                    Avoid triggering issues for casual praise or general movie questions.
                """.trimIndent()

                val api = com.example.data.remote.OllamaClient.getApiService(ollamaHost.value)
                val requestMessages = mutableListOf(com.example.data.remote.OllamaChatMessage("system", systemPrompt))
                requestMessages.addAll(currentChat)

                val request = com.example.data.remote.OllamaChatRequest(
                    messages = requestMessages
                )
                val response = api.chat(request)
                
                var replyContent = response.message.content
                
                // --- Agentic Log Persistence (Local Sync) ---
                logConversationLocally(userMessage, replyContent)

                // Intercept Self-Evolution Request
                if (replyContent.contains("<github_issue>")) {
                    val jsonStr = replyContent.substringAfter("<github_issue>").substringBefore("</github_issue>").trim()
                    replyContent = replyContent.replace(Regex("<github_issue>.*</github_issue>", RegexOption.DOT_MATCHES_ALL), "").trim()
                    
                    if (githubToken.value.isNotEmpty()) {
                        createGithubIssue(jsonStr)
                        replyContent += "\n\n*(Agentic Protocol: I have submitted this feature request directly to the GitHub repository to trigger the autonomous coding pipeline.)*"
                    } else {
                        replyContent += "\n\n*(Agentic Protocol: I generated the feature request, but your GitHub Token is missing in Settings. Please add it to enable Self-Evolving capabilities.)*"
                    }
                }

                currentChat.add(com.example.data.remote.OllamaChatMessage("assistant", replyContent))
                _chatMessages.value = currentChat
            } catch (e: Exception) {
                val errorMsg = if (e is java.net.ConnectException || e is java.net.SocketTimeoutException || e is java.net.UnknownHostException) {
                    "Unable to connect to local Ollama instance at ${ollamaHost.value}:11434. Please ensure your laptop is running Ollama (`OLLAMA_HOST=0.0.0.0 ollama serve`) on the local network, or update the Ollama Host IP in Settings."
                } else {
                    "Error connecting to local Ollama instance: ${e.message}"
                }
                currentChat.add(com.example.data.remote.OllamaChatMessage("assistant", errorMsg))
                _chatMessages.value = currentChat
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    private fun logConversationLocally(user: String, assistant: String) {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val logFile = File(downloadsDir, "agent_conversations.jsonl")
            val entry = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("user", user)
                put("assistant", assistant)
            }
            logFile.appendText(entry.toString() + "\n")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to log conversation", e)
        }
    }

    private fun createGithubIssue(jsonPayload: String) {
        try {
            val url = URL("https://api.github.com/repos/good-enough-productions/Streamwise/issues")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer ${githubToken.value}")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            connection.outputStream.use { os ->
                val input = jsonPayload.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            android.util.Log.d(TAG, "GitHub Issue Creation Response: $responseCode")
            if (responseCode in 200..299) {
                _statusMessage.value = "Feature request submitted to GitHub successfully!"
            } else {
                _statusMessage.value = "Failed to create GitHub issue: $responseCode"
            }
            connection.disconnect()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "GitHub Issue Error", e)
        }
    }

    fun exportToObsidian() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allItems = repository.allMediaItems.first()
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val exportDir = File(downloadsDir, "StreamwiseVault")
                if (!exportDir.exists()) exportDir.mkdirs()

                allItems.forEach { item ->
                    val safeTitle = item.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    val file = File(exportDir, "$safeTitle.md")
                    
                    val content = java.lang.StringBuilder().apply {
                        appendLine("---")
                        appendLine("title: \"${item.title}\"")
                        appendLine("status: \"${item.status}\"")
                        if (item.rating != null) appendLine("rating: ${item.rating}")
                        if (!item.genres.isNullOrEmpty()) appendLine("genres: \"${item.genres}\"")
                        if (!item.importSource.isNullOrEmpty()) appendLine("source: \"${item.importSource}\"")
                        appendLine("---")
                        appendLine()
                        appendLine("# ${item.title}")
                        appendLine()
                        if (!item.overview.isNullOrEmpty()) {
                            appendLine("## Overview")
                            appendLine(item.overview)
                            appendLine()
                        }
                        if (!item.userNotes.isNullOrEmpty()) {
                            appendLine("## Personal Notes")
                            appendLine(item.userNotes)
                            appendLine()
                        }
                        if (!item.trivia.isNullOrEmpty()) {
                            appendLine(item.trivia)
                        }
                    }
                    file.writeText(content.toString())
                }
                
                _statusMessage.value = "Exported ${allItems.size} titles to Downloads/StreamwiseVault"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    fun startDeviceDiscovery() {
        viewModelScope.launch {
            CastingManager.discoverDevices(getApplication())
        }
    }

    fun castToDevice(device: CastDevice, mediaItem: MediaItem) {
        viewModelScope.launch {
            val url = mediaItem.sharedUrl ?: return@launch
            CastingManager.castUrl(device, url)
            _statusMessage.value = "Casting \"${mediaItem.title}\" to ${device.name}..."
        }
    }

    /**
     * Scans Room database when app lifecycle resumes.
     * If an item is in "INTENDING_TO_WATCH" status, triggers check-in bottom sheet configuration.
     */
    fun checkForIntendingToWatchOnResume() {
        viewModelScope.launch {
            val intendingItems = repository.getIntendingToWatchItems()
            if (intendingItems.isNotEmpty()) {
                // Select the most recent intending watch item to prompt
                _activeCheckInItem.value = intendingItems.first()
            }
        }
    }

    /**
     * User clicks a title to signify intent to start watching.
     * Prompts foreground watcher to monitor their status next time they open the app.
     */
    fun startIntendingToWatch(item: MediaItem) {
        viewModelScope.launch {
            val updated = item.copy(status = MediaStatus.INTENDING_TO_WATCH.name, updatedAt = System.currentTimeMillis())
            repository.updateMediaItem(updated)
            _statusMessage.value = "Watching instruction initialized: Let's check in after!"
        }
    }

    /**
     * User clicks "Watch" to signify intent and directly launch into the streaming service.
     * Marks intent for post-watch check-in and triggers native app deep-link or universal search.
     */
    fun launchAndIntendToWatch(context: android.content.Context, item: MediaItem) {
        viewModelScope.launch {
            val updated = item.copy(status = MediaStatus.INTENDING_TO_WATCH.name, updatedAt = System.currentTimeMillis())
            repository.updateMediaItem(updated)

            // Resolve primary available provider
            val allProv = allProviders.value
            val activeOrFreeIds = allProv.filter { it.isActive || it.costPerMonth == 0.0 }.map { it.id }.toSet()
            val primaryProviderId = item.providersList.firstOrNull { activeOrFreeIds.contains(it) } 
                ?: item.providersList.firstOrNull()

            val result = com.example.data.util.StreamingAppLauncher.launchStreamingApp(
                context = context,
                providerId = primaryProviderId,
                movieTitle = item.title
            )
            _statusMessage.value = result.message
        }
    }

    /**
     * Action 1: User finished the movie/show successfully on a provider.
     * Log the watch duration to aggregate exact monthly analytics.
     */
    fun logSessionFinished(item: MediaItem, providerId: String?, durationMinutes: Int, notes: String?) {
        viewModelScope.launch {
            repository.addWatchSession(
                mediaItemId = item.id,
                mediaItemTitle = item.title,
                providerId = providerId,
                durationMinutes = durationMinutes,
                notes = notes
            )
            _activeCheckInItem.value = null
            _statusMessage.value = "Spectacular! \"${item.title}\" logged to History."
        }
    }

    /**
     * Action 2: User watched some of it but did not finish yet.
     * Keeps item in local watchlist, but logs the logged partial watch time.
     */
    fun logSessionPartial(item: MediaItem, providerId: String?, durationMinutes: Int, notes: String?) {
        viewModelScope.launch {
            // Add session log representing partial time
            repository.addWatchSession(
                mediaItemId = item.id,
                mediaItemTitle = item.title,
                providerId = providerId,
                durationMinutes = durationMinutes,
                notes = notes ?: "Partial viewing check-in log."
            )
            // Revert state back to Active Watchlist so user can watch again later
            val updated = item.copy(status = MediaStatus.WATCHLIST.name, updatedAt = System.currentTimeMillis())
            repository.updateMediaItem(updated)
            _activeCheckInItem.value = null
            _statusMessage.value = "logged partial $durationMinutes mins. Active in Watchlist."
        }
    }

    /**
     * Action 3: User watched something else instead.
     * Encourages manual entry of that custom item, and reverts current item to standard watchlist state.
     */
    fun watchedSomethingElse(item: MediaItem, customTitle: String, providerId: String?, durationMinutes: Int) {
        viewModelScope.launch {
            // Revert original item status to standard watchlist state
            val updatedOriginal = item.copy(status = MediaStatus.WATCHLIST.name, updatedAt = System.currentTimeMillis())
            repository.updateMediaItem(updatedOriginal)

            // Add the new title directly as a completed item in one-click history log
            val customItem = MediaItem(
                title = customTitle,
                status = MediaStatus.WATCHED.name,
                providerIds = providerId
            )
            val newId = repository.insertMediaItem(customItem)

            repository.addWatchSession(
                mediaItemId = newId,
                mediaItemTitle = customTitle,
                providerId = providerId,
                durationMinutes = durationMinutes,
                notes = "Ad-hoc entry logged from Check-In feedback loop."
            )

            _activeCheckInItem.value = null
            _statusMessage.value = "Logged standard session for \"$customTitle\"!"
        }
    }

    /**
     * Action 4: User didn't watch anything.
     * Simply returns the item to normal Watchlist state and clears check-in.
     */
    fun clearIntentFlag(item: MediaItem) {
        viewModelScope.launch {
            val updated = item.copy(status = MediaStatus.WATCHLIST.name, updatedAt = System.currentTimeMillis())
            repository.updateMediaItem(updated)
            _activeCheckInItem.value = null
            _statusMessage.value = "Intent checked off. Watchlist restored."
        }
    }

    /**
     * Handles ACTION_SEND Intent shares imported from Safari, Chrome, IMDB or letterboxd.
     * Cleans titles and inserts into database under PENDING_METADATA.
     */
    fun handleAddSharedContent(sharedText: String) {
        viewModelScope.launch {
            val potentialTitle = try {
                extractMovieTitleFromSharedText(sharedText)
            } catch (e: Exception) {
                "Shared Item (${System.currentTimeMillis() % 100})"
            }

            val item = MediaItem(
                title = potentialTitle,
                sharedUrl = extractUrlFromText(sharedText),
                status = MediaStatus.PENDING_METADATA.name
            )
            repository.insertMediaItem(item)
            enqueueTmdbSync(showMessage = false)
            _statusMessage.value = "\"$potentialTitle\" successfully added via Quick Share!"
        }
    }

    /**
     * Inserts standard custom entries manually from inside the App home view.
     */
    fun addCustomWatchlistItem(title: String, associatedProviders: List<String>) {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            val providerString = if (associatedProviders.isEmpty()) null else associatedProviders.joinToString(",")
            val item = MediaItem(
                title = title.trim(),
                status = MediaStatus.WATCHLIST.name,
                providerIds = providerString
            )
            repository.insertMediaItem(item)
            enqueueTmdbSync(showMessage = false)
            _statusMessage.value = "\"${title.trim()}\" added to watchlist!"
        }
    }

    /**
     * Supports multiline add where each non-empty line is treated as one title.
     */
    fun addCustomWatchlistItemsBulk(
        multilineTitles: String, 
        associatedProviders: List<String>,
        userNotes: String? = null,
        importSource: String? = null
    ) {
        viewModelScope.launch {
            val titles = multilineTitles
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .toList()

            if (titles.isEmpty()) return@launch

            val providerString = if (associatedProviders.isEmpty()) null else associatedProviders.joinToString(",")
            titles.forEach { title ->
                val item = MediaItem(
                    title = title,
                    status = MediaStatus.WATCHLIST.name,
                    providerIds = providerString,
                    userNotes = userNotes,
                    importSource = importSource
                )
                repository.insertMediaItem(item)
            }

            enqueueTmdbSync(showMessage = false)
            _statusMessage.value = if (titles.size == 1) {
                "\"${titles.first()}\" added to watchlist!"
            } else {
                "Added ${titles.size} titles to watchlist. Matching from TMDB started."
            }
        }
    }

    /**
     * Toggles subscription activity states in Settings/Budget panels to filter lists dynamically.
     */
    fun toggleStreamingProvider(providerId: String, isActive: Boolean) {
        viewModelScope.launch {
            val provider = allProviders.value.find { it.id == providerId } ?: return@launch
            val updated = provider.copy(isActive = isActive, updatedAt = System.currentTimeMillis())
            repository.updateStreamingProvider(updated)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private var isSyncing = false

    private fun mapTmdbProviders(providers: List<com.example.data.remote.TmdbProvider>): String? {
        val localIds = mutableListOf<String>()
        for (p in providers) {
            val name = p.providerName.lowercase()
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

    fun syncWatchlistMetadata(forceAll: Boolean = false) {
        if (isSyncing) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isSyncing = true
                val runtimeKey = userPreferences.tmdbApiKey
                val buildTimeKey = com.example.BuildConfig.TMDB_API_KEY
                val key = if (runtimeKey.isNotEmpty() && runtimeKey != "MY_TMDB_API_KEY") runtimeKey else buildTimeKey
                if (key.isEmpty() || key == "MY_TMDB_API_KEY") {
                    _statusMessage.value = "Please configure your TMDB API Key in Settings."
                    return@launch
                }

                val allItems = repository.allMediaItems.first()
                val targetItems = if (forceAll) {
                    allItems.filter { it.status == MediaStatus.WATCHLIST.name || it.status == MediaStatus.PENDING_METADATA.name }
                } else {
                    allItems.filter { 
                        (it.status == MediaStatus.PENDING_METADATA.name) ||
                        ((it.status == MediaStatus.WATCHLIST.name) &&
                        (it.tmdbId.isNullOrEmpty() || it.imageUrl.isNullOrEmpty()))
                    }
                }

                if (targetItems.isEmpty()) {
                    _statusMessage.value = "Watchlist availability is up to date."
                    return@launch
                }

                _statusMessage.value = "Syncing ${targetItems.size} titles from TMDB..."

                val genreMap = try {
                    val resp = com.example.data.remote.TmdbClient.tmdbApiService.getGenreList(key)
                    resp.genres.associate { it.id to it.name }
                } catch (e: Exception) {
                    emptyMap<Int, String>()
                }

                var updatedCount = 0
                for (item in targetItems) {
                    if (MediaTitleSanitizer.isNonMovieEpisode(item.title) ||
                        MediaTitleSanitizer.isNonMovieEpisode(MediaTitleSanitizer.cleanCandidateTitle(item.title))) {
                        repository.deleteMediaItem(item)
                        continue
                    }

                    val cleanTitle = MediaTitleSanitizer.cleanCandidateTitle(item.title)
                    try {
                        val searchResp = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(key, cleanTitle)
                        var match = searchResp.results.firstOrNull()
                        if (match == null && cleanTitle.contains(":")) {
                            val prefix = cleanTitle.substringBefore(":").trim()
                            if (prefix.length >= 3) {
                                match = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(key, prefix).results.firstOrNull()
                            }
                        }

                        if (match != null) {
                            val movieId = match.id
                            var providersString: String? = null
                            try {
                                val provResp = com.example.data.remote.TmdbClient.tmdbApiService.getWatchProviders(movieId, key)
                                val us = provResp.results?.get("US")
                                val list = mutableListOf<com.example.data.remote.TmdbProvider>()
                                us?.flatrate?.let { list.addAll(it) }
                                us?.free?.let { list.addAll(it) }
                                us?.ads?.let { list.addAll(it) }
                                providersString = mapTmdbProviders(list)
                            } catch (e: Exception) {
                                android.util.Log.w(TAG, "Provider fetch failed for ${item.title}: ${e.message}")
                            }

                            val genres = match.genreIds?.mapNotNull { genreMap[it] }?.joinToString(", ") ?: item.genres
                            val poster = match.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: item.imageUrl
                            val rating = match.voteAverage ?: item.rating
                            val overview = match.overview?.ifBlank { item.overview } ?: item.overview

                            val updated = item.copy(
                                title = match.title,
                                tmdbId = movieId.toString(),
                                providerIds = providersString ?: item.providerIds,
                                imageUrl = poster,
                                rating = rating,
                                overview = overview,
                                genres = genres,
                                releaseDate = match.releaseDate ?: item.releaseDate,
                                status = if (item.status == MediaStatus.PENDING_METADATA.name) MediaStatus.WATCHLIST.name else item.status,
                                updatedAt = System.currentTimeMillis()
                            )
                            repository.updateMediaItem(updated)
                            updatedCount++
                        } else {
                            // Try TV search if not found in movies
                            try {
                                val tvResp = com.example.data.remote.TmdbClient.tmdbApiService.searchTv(key, cleanTitle)
                                val tvMatch = tvResp.results.firstOrNull()
                                if (tvMatch != null) {
                                    val tvId = tvMatch.id
                                    var tvProvString: String? = null
                                    try {
                                        val tvProvResp = com.example.data.remote.TmdbClient.tmdbApiService.getTvWatchProviders(tvId, key)
                                        val us = tvProvResp.results?.get("US")
                                        val list = mutableListOf<com.example.data.remote.TmdbProvider>()
                                        us?.flatrate?.let { list.addAll(it) }
                                        us?.free?.let { list.addAll(it) }
                                        us?.ads?.let { list.addAll(it) }
                                        tvProvString = mapTmdbProviders(list)
                                    } catch (e: Exception) {
                                        android.util.Log.w(TAG, "TV provider fetch failed for ${item.title}: ${e.message}")
                                    }

                                    val tvGenres = tvMatch.genreIds?.mapNotNull { genreMap[it] }?.joinToString(", ") ?: item.genres
                                    val tvPoster = tvMatch.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: item.imageUrl

                                    val updated = item.copy(
                                        title = tvMatch.name,
                                        tmdbId = tvId.toString(),
                                        providerIds = tvProvString ?: item.providerIds,
                                        imageUrl = tvPoster,
                                        rating = tvMatch.voteAverage ?: item.rating,
                                        overview = tvMatch.overview?.ifBlank { item.overview } ?: item.overview,
                                        genres = tvGenres,
                                        releaseDate = tvMatch.firstAirDate ?: item.releaseDate,
                                        status = if (item.status == MediaStatus.PENDING_METADATA.name) MediaStatus.WATCHLIST.name else item.status,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    repository.updateMediaItem(updated)
                                    updatedCount++
                                } else {
                                    // Could not match to TMDB movie or TV:
                                    if (item.imageUrl.isNullOrBlank() || item.tmdbId.isNullOrBlank()) {
                                        // Purge non-movie / unmatchable item from watchlist
                                        repository.deleteMediaItem(item)
                                    } else {
                                        val fallback = item.copy(
                                            status = MediaStatus.WATCHLIST.name,
                                            providerIds = item.providerIds ?: "none",
                                            updatedAt = System.currentTimeMillis()
                                        )
                                        repository.updateMediaItem(fallback)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w(TAG, "TV search failed for ${item.title}: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Failed syncing item ${item.title}: ${e.message}")
                    }
                    kotlinx.coroutines.delay(120)
                }

                _statusMessage.value = "Synced $updatedCount titles with TMDB streaming availability!"
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in direct TMDB sync: ${e.message}", e)
                _statusMessage.value = "TMDB sync encountered an issue: ${e.message}"
            } finally {
                isSyncing = false
            }
        }
    }

    fun triggerImmediateSync() {
        syncPodcastRecommendations()
        syncWatchlistMetadata(forceAll = false)
        enqueueTmdbSync(showMessage = false)
    }

    private fun enqueueTmdbSync(showMessage: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<AvailabilitySyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            AvailabilitySyncWorker.ONE_TIME_WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
        if (showMessage) {
            _statusMessage.value = "Fetching streaming availability from TMDB\u2026"
        }
    }

    fun deleteItem(item: MediaItem) {
        viewModelScope.launch {
            repository.deleteMediaItem(item)
            _statusMessage.value = "\"${item.title}\" deleted."
        }
    }

    // --- Helper Utilities for Title/Url Extractor Parsing ---

    private fun extractMovieTitleFromSharedText(text: String): String {
        // Standard shared texts: "The Godfather (1972) - IMDb https://www.imdb.com/title/..." 
        // Or simple: "https://www.imdb.com/title/tt0068646/"
        val lines = text.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return "Shared Stream Title"

        val firstLine = lines.first()
        // If first line is a pure URL, try to parse its end path, otherwise use it
        if (firstLine.startsWith("http://") || firstLine.startsWith("https://")) {
            val uri = Uri.parse(firstLine)
            val pathSegments = uri.pathSegments
            return if (pathSegments.isNotEmpty()) {
                val lastSeg = pathSegments.last().replace("-", " ").capitalizeWords()
                if (lastSeg.matches(Regex("[A-Za-z0-9 ]+"))) lastSeg else "Shared Link Title"
            } else {
                "Shared Online Media"
            }
        }

        // Often shares come with title first, and links at the end
        // e.g. "Take a look at Severance on IMDb: https://..."
        var parsed = firstLine
        val imdbPrefix = "Check out "
        if (parsed.startsWith(imdbPrefix, ignoreCase = true)) {
            parsed = parsed.substring(imdbPrefix.length)
        }
        val onImdbIdx = parsed.indexOf(" on IMDb", ignoreCase = true)
        if (onImdbIdx != -1) {
            parsed = parsed.substring(0, onImdbIdx)
        }

        // Strip years or URLs at the end
        val urlIdx = parsed.indexOf("http")
        if (urlIdx != -1) {
            parsed = parsed.substring(0, urlIdx).trim()
        }

        return parsed.trim()
    }

    private fun extractUrlFromText(text: String): String? {
        val words = text.split(Regex("\\s+"))
        return words.find { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
    }

    fun updateMediaItem(item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMediaItem(item)
        }
    }

    fun setServiceUsed(item: MediaItem, providerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = item.copy(
                providerIds = providerId,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateMediaItem(updated)
        }
    }

    /**
     * Enriches logged watched films with official TMDB ratings, release dates, and genres.
     */
    fun enrichWatchedVaultRatings(batchLimit: Int = 150) {
        if (_isEnrichingVault.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isEnrichingVault.value = true
            _vaultEnrichProgress.value = "Preparing vault enrichment..."
            try {
                val runtimeKey = userPreferences.tmdbApiKey
                val buildTimeKey = com.example.BuildConfig.TMDB_API_KEY
                val key = if (runtimeKey.isNotEmpty() && runtimeKey != "MY_TMDB_API_KEY") runtimeKey else buildTimeKey
                if (key.isEmpty() || key == "MY_TMDB_API_KEY") {
                    _statusMessage.value = "Please configure your TMDB API Key in Settings to enrich ratings."
                    _isEnrichingVault.value = false
                    return@launch
                }

                val allItems = repository.allMediaItems.first()
                val unratedWatched = allItems.filter { 
                    it.status == MediaStatus.WATCHED.name && ((it.rating ?: 0.0) <= 0.0) 
                }.take(batchLimit)

                if (unratedWatched.isEmpty()) {
                    _statusMessage.value = "All watched films are rated!"
                    _vaultEnrichProgress.value = "Vault ratings are fully enriched."
                    _isEnrichingVault.value = false
                    return@launch
                }

                _vaultEnrichProgress.value = "Enriching ${unratedWatched.size} films with TMDB ratings..."

                val genreMap = try {
                    val resp = com.example.data.remote.TmdbClient.tmdbApiService.getGenreList(key)
                    resp.genres.associate { it.id to it.name }
                } catch (e: Exception) {
                    emptyMap<Int, String>()
                }

                var enrichedCount = 0
                val updatedList = mutableListOf<MediaItem>()

                for ((idx, item) in unratedWatched.withIndex()) {
                    _vaultEnrichProgress.value = "Enriching ${idx + 1}/${unratedWatched.size}: ${item.title}"
                    // Respect TMDB rate-limiting
                    delay(250)

                    val cleanTitle = MediaTitleSanitizer.cleanCandidateTitle(item.title)
                    try {
                        val searchResp = com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(key, cleanTitle)
                        val match = searchResp.results.firstOrNull() ?: if (cleanTitle.contains(":")) {
                            val prefix = cleanTitle.substringBefore(":").trim()
                            if (prefix.length >= 3) {
                                com.example.data.remote.TmdbClient.tmdbApiService.searchMovie(key, prefix).results.firstOrNull()
                            } else null
                        } else null

                        if (match != null && match.voteAverage != null && match.voteAverage > 0.0) {
                            val genres = match.genreIds?.mapNotNull { genreMap[it] }?.joinToString(", ")
                            val poster = match.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                            val updated = item.copy(
                                rating = match.voteAverage,
                                genres = item.genres ?: genres,
                                imageUrl = item.imageUrl ?: poster,
                                tmdbId = item.tmdbId ?: match.id.toString(),
                                releaseDate = item.releaseDate ?: match.releaseDate,
                                updatedAt = System.currentTimeMillis()
                            )
                            updatedList.add(updated)
                            enrichedCount++
                        }
                    } catch (e: Exception) {
                        Log.e("StreamViewModel", "Error enriching ${item.title}", e)
                    }

                    if (updatedList.size >= 25) {
                        repository.updateMediaItems(updatedList.toList())
                        updatedList.clear()
                    }
                }

                if (updatedList.isNotEmpty()) {
                    repository.updateMediaItems(updatedList.toList())
                    updatedList.clear()
                }

                _statusMessage.value = "Enriched $enrichedCount watched films with TMDB ratings."
                _vaultEnrichProgress.value = "Enriched $enrichedCount films."
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Error during vault rating enrichment", e)
                _statusMessage.value = "Enrichment error: ${e.message}"
            } finally {
                _isEnrichingVault.value = false
            }
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

/**
 * Custom Factory allowing constructor injection for MediaRepository without generating runtime build/Hilt crashes.
 */
class StreamViewModelFactory(
    private val application: Application,
    private val repository: MediaRepository,
    private val userPreferences: UserPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StreamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StreamViewModel(application, repository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
