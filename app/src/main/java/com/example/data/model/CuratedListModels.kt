package com.example.data.model

/**
 * Represents an individual film entry within a curated or 3rd-party list.
 */
data class CuratedListItem(
    val title: String,
    val year: Int? = null,
    val badge: String? = null,         // e.g., "1972 Winner", "#1 AFI", "Ep 142"
    val director: String? = null,
    val note: String? = null,
    val tmdbId: String? = null,
    val defaultProviders: List<String> = emptyList()
)

/**
 * The high-level categories for organizing 3rd-party cinema lists.
 */
enum class CuratedListCategory(val displayName: String, val icon: String) {
    ALL("All Lists", "📜"),
    VIBES("Moods & Vibes", "✨"),
    RECENT("Modern & Heat", "🔥"),
    AWARDS("Awards & Oscars", "🏆"),
    CANON("Cinema Canon", "🏛️"),
    PODCASTS("Podcasts", "🎙️"),
    CUSTOM("Custom & Ad-Hoc", "🔗")
}

/**
 * A curated or ad-hoc 3rd-party movie list.
 */
data class CuratedList(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: CuratedListCategory,
    val emoji: String,
    val description: String,
    val items: List<CuratedListItem> = emptyList(),
    val isCustom: Boolean = false
)

/**
 * A film from a 3rd-party list dynamically cross-referenced with the user's
 * local database: whether it is on their personal watchlist, already watched,
 * and which of their paid streaming services it is available on.
 */
data class EnrichedCuratedItem(
    val item: CuratedListItem,
    val isWatched: Boolean,
    val userRating: Double? = null,
    val watchedAt: Long? = null,
    val isInWatchlist: Boolean,
    val isStarred: Boolean = false,
    val matchingMediaItem: MediaItem? = null,
    val streamingProviders: List<String> = emptyList(),          // e.g. ["max", "netflix"]
    val activeProviders: List<StreamingProvider> = emptyList(),    // User's active paid subscriptions that offer this film
    val isAvailableOnMyServices: Boolean = false
) {
    val displayYear: String
        get() = item.year?.toString() ?: matchingMediaItem?.releaseYear?.toString() ?: ""

    val displayTitle: String
        get() = matchingMediaItem?.title ?: item.title
}

/**
 * Statistical summary computed for a curated list against user's personal vault.
 */
data class CuratedListStats(
    val totalCount: Int,
    val watchedCount: Int,
    val watchlistCount: Int,
    val streamingNowCount: Int
) {
    val watchedPercent: Int
        get() = if (totalCount > 0) ((watchedCount.toFloat() / totalCount) * 100).toInt() else 0
}
