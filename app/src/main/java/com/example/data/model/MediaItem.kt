package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaStatus {
    PENDING_METADATA,       // Shared link added, waiting for TMDB API metadata retrieval
    WATCHLIST,              // Active in user's watchlist
    INTENDING_TO_WATCH,     // Selected for viewing, checking in on foreground return
    WATCHED                 // Already watched and logged in history
}

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sharedUrl: String? = null,
    val status: String = MediaStatus.PENDING_METADATA.name, // Saved as String for database robustness
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tmdbId: String? = null,
    val imageUrl: String? = null, // Poster path URL
    val rating: Double? = null,
    val overview: String? = null,
    // Agent-synthesized cultural trivia, focus topics, and research notes for pre-watch strategy
    val trivia: String? = null,
    // User's own personal notes about the movie
    val userNotes: String? = null,
    // Source from where the movie was imported (e.g., "Podcast: The Big Picture")
    val importSource: String? = null,
    // Comma-separated list of genres (e.g., "Sci-Fi, Drama")
    val genres: String? = null,
    // Latest timestamp when this movie was watched
    val watchedAt: Long? = null,
    // Comma-separated list of active provider IDs (e.g., "netflix,hulu,max") available for this media item
    val providerIds: String? = null,
    // User's personal rating (0.5 to 5.0 stars)
    val userRating: Double? = null,
    // Letterboxd rewatch indicator
    val isRewatch: Boolean = false,
    // Letterboxd URI link
    val letterboxdUri: String? = null,
    // Google Sheet cloud sync indicator
    val syncedToSheet: Boolean = false,
    // Runtime duration in minutes for <90m and <120m filtering
    val runtimeMinutes: Int? = null,
    // Release year (e.g., "1995")
    val releaseYear: String? = null,
    // Media type: "MOVIE" or "TV"
    val mediaType: String = "MOVIE",
    // TV Show metadata
    val totalSeasons: Int? = null,
    val totalEpisodes: Int? = null,
    val lastWatchedSeason: Int? = null,
    val lastWatchedEpisode: Int? = null
) {
    // Utility to parse provider IDs array
    val providersList: List<String>
        get() = providerIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    val isTvShow: Boolean
        get() = mediaType.equals("TV", ignoreCase = true)

    val isPodcastRec: Boolean
        get() = importSource?.startsWith("Podcast", ignoreCase = true) == true
}
