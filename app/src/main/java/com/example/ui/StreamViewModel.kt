package com.example.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferencesManager
import com.example.data.local.ProviderUsageStats
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.StreamingProvider
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class StreamViewModel(
    private val repository: MediaRepository,
    private val userPreferences: UserPreferencesManager
) : ViewModel() {

    // --- State Expositions ---

    // All registered media items
    val allMediaItems: StateFlow<List<MediaItem>> = repository.allMediaItems
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

    fun saveTmdbApiKey(key: String) {
        userPreferences.tmdbApiKey = key
        _tmdbApiKey.value = key.trim()
        _statusMessage.value = if (key.isBlank()) "TMDB API key cleared." else "TMDB API key saved."
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
            _statusMessage.value = "\"${title.trim()}\" added to watchlist!"
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
    private val repository: MediaRepository,
    private val userPreferences: UserPreferencesManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StreamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StreamViewModel(repository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
