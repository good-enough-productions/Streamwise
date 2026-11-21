package com.example.data.model

data class GeminiAnalysisResult(
    val headline: String,
    val narrative: String,
    val themes: List<String>,
    val auteurConnections: List<String>,
    val recommendations: List<Pair<String, String>>
)

data class PodcastEpisode(
    val id: String,
    val showTitle: String,
    val episodeTitle: String,
    val duration: String,
    val description: String,
    val date: String,
    val podcastUrl: String
)

data class MovieNewsItem(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val source: String,
    val date: String
)

data class NextWatchRecommendation(
    val tmdbId: String?,
    val title: String,
    val year: Int?,
    val posterUrl: String?,
    val rating: Double?,
    val overview: String?,
    val reason: String,
    val sourceSeed: String,
    val isInWatchlist: Boolean = false,
    val isWatched: Boolean = false,
    val isStarred: Boolean = false,
    val matchingMediaItem: MediaItem? = null,
    val availableProviders: List<String> = emptyList()
)

data class UniversalMovieResult(
    val tmdbId: String?,
    val title: String,
    val year: Int?,
    val releaseDate: String?,
    val posterUrl: String?,
    val rating: Double?,
    val overview: String?,
    val genres: String?,
    val isInWatchlist: Boolean = false,
    val isWatched: Boolean = false,
    val isStarred: Boolean = false,
    val matchingMediaItem: MediaItem? = null,
    val mediaType: String = "Movie"
)

data class PersonSearchCandidate(
    val id: Int,
    val name: String,
    val profileUrl: String?,
    val department: String?,
    val knownForSummary: String
)

data class PersonFilmographyItem(
    val tmdbId: String,
    val title: String,
    val releaseDate: String?,
    val year: Int?,
    val posterUrl: String?,
    val rating: Double?,
    val overview: String?,
    val roleLabel: String,
    val actorAgeAtRelease: Int?,
    val isWatched: Boolean,
    val isInWatchlist: Boolean,
    val isStarred: Boolean,
    val matchingMediaItem: MediaItem? = null
)

data class PersonFilmographyResult(
    val personId: Int,
    val name: String,
    val department: String?,
    val birthday: String?,
    val deathday: String?,
    val placeOfBirth: String?,
    val profileUrl: String?,
    val items: List<PersonFilmographyItem>,
    val watchedCount: Int,
    val totalCount: Int
) {
    val watchedPercent: Int
        get() = if (totalCount > 0) ((watchedCount.toFloat() / totalCount) * 100).toInt() else 0
}
