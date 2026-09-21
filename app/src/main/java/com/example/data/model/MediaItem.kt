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
    // Official release date from TMDB (e.g., "1989-08-09")
    val releaseDate: String? = null
) {
    // Utility to parse provider IDs array
    val providersList: List<String>
        get() = providerIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() && it != "none" } ?: emptyList()

    // 4-digit release year extracted from releaseDate or fallback title
    val releaseYear: Int?
        get() = releaseDate?.take(4)?.toIntOrNull()

    /**
     * Determines whether this item was manually created by the user, has user-curated notes/tags,
     * or originated from a non-automated source. User-protected items are NEVER deleted by background
     * integrity sweeps, deduplication scripts, or unmatchable TMDB queries.
     */
    val isUserProtected: Boolean
        get() {
            if (!userNotes.isNullOrBlank()) return true
            if (!providerIds.isNullOrBlank() && providerIds != "none") return true
            val src = importSource?.trim() ?: ""
            if (src.equals("Manual Entry", ignoreCase = true) ||
                src.equals("User Added", ignoreCase = true) ||
                src.startsWith("Letterboxd", ignoreCase = true) ||
                src.startsWith("Gemini", ignoreCase = true) ||
                src.startsWith("Shared Link", ignoreCase = true) ||
                src.isBlank()) {
                return true
            }
            return false
        }
}
