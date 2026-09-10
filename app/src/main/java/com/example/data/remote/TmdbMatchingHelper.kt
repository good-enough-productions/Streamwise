package com.example.data.remote

import android.util.Log
import com.example.data.util.MediaTitleSanitizer
import java.util.Locale

object TmdbMatchingHelper {

    private const val TAG = "TmdbMatchingHelper"

    /**
     * Normalizes a title for robust deduplication and comparison:
     * - Lowercases
     * - Strips leading articles ("the", "a", "an")
     * - Strips all non-alphanumeric characters
     */
    fun normalizeTitle(raw: String): String {
        var s = raw.lowercase(Locale.US).trim()
        s = s.replace(Regex("""^(?:the|a|an)\s+""", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("""[^a-z0-9]"""), "")
        return s
    }

    /**
     * Given search results from TMDB, selects the best matching candidate:
     * 1. Exact case-insensitive match on title.
     * 2. Normalized title match (ignoring articles and punctuation).
     * 3. Release year match (if target year is provided and within range).
     * 4. Prefix / Substring match.
     * 5. Fallback to first result.
     */
    fun findBestMovieMatch(
        results: List<TmdbSearchResult>,
        queryTitle: String,
        targetYear: String? = null
    ): TmdbSearchResult? {
        if (results.isEmpty()) return null

        val cleanQuery = queryTitle.trim()
        val normQuery = normalizeTitle(cleanQuery)
        val cleanYear = targetYear?.trim()?.take(4)

        // 1. Exact match AND year match (if year provided)
        if (!cleanYear.isNullOrBlank()) {
            val exactYearMatch = results.firstOrNull {
                it.title.trim().equals(cleanQuery, ignoreCase = true) && it.releaseDate?.take(4) == cleanYear
            }
            if (exactYearMatch != null) return exactYearMatch

            // 2. Normalized match AND year match
            val normYearMatch = results.firstOrNull {
                normalizeTitle(it.title) == normQuery && it.releaseDate?.take(4) == cleanYear
            }
            if (normYearMatch != null) return normYearMatch
        }

        // 3. Exact case-insensitive match (e.g. "Network" matches "Network", not "The Social Network")
        val exactMatch = results.firstOrNull { it.title.trim().equals(cleanQuery, ignoreCase = true) }
        if (exactMatch != null) return exactMatch

        // 4. Normalized title match (e.g. "The Social Network" vs "Social Network")
        val normMatch = results.firstOrNull { normalizeTitle(it.title) == normQuery }
        if (normMatch != null) return normMatch

        // 5. Year match if year provided with substring/contains
        if (!cleanYear.isNullOrBlank()) {
            val yearMatch = results.firstOrNull { r ->
                val rYear = r.releaseDate?.take(4)
                rYear == cleanYear && (
                    normalizeTitle(r.title).contains(normQuery) || 
                    normQuery.contains(normalizeTitle(r.title))
                )
            }
            if (yearMatch != null) return yearMatch
        }

        // 6. Substring / contains match
        val containsMatch = results.firstOrNull {
            val normR = normalizeTitle(it.title)
            normR.contains(normQuery) || normQuery.contains(normR)
        }
        if (containsMatch != null) return containsMatch

        // 7. Fallback to first result
        return results.first()
    }

    /**
     * Finds best TV match from results.
     */
    fun findBestTvMatch(
        results: List<TmdbTvSearchResult>,
        queryTitle: String,
        targetYear: String? = null
    ): TmdbTvSearchResult? {
        if (results.isEmpty()) return null

        val cleanQuery = queryTitle.trim()
        val normQuery = normalizeTitle(cleanQuery)
        val cleanYear = targetYear?.trim()?.take(4)

        if (!cleanYear.isNullOrBlank()) {
            val exactYearMatch = results.firstOrNull {
                it.name.trim().equals(cleanQuery, ignoreCase = true) && it.firstAirDate?.take(4) == cleanYear
            }
            if (exactYearMatch != null) return exactYearMatch

            val normYearMatch = results.firstOrNull {
                normalizeTitle(it.name) == normQuery && it.firstAirDate?.take(4) == cleanYear
            }
            if (normYearMatch != null) return normYearMatch
        }

        val exactMatch = results.firstOrNull { it.name.trim().equals(cleanQuery, ignoreCase = true) }
        if (exactMatch != null) return exactMatch

        val normMatch = results.firstOrNull { normalizeTitle(it.name) == normQuery }
        if (normMatch != null) return normMatch

        if (!cleanYear.isNullOrBlank()) {
            val yearMatch = results.firstOrNull { it.firstAirDate?.take(4) == cleanYear }
            if (yearMatch != null) return yearMatch
        }

        return results.first()
    }

    /**
     * Executes smart TMDB movie search with fallback strategies:
     * - Searches with query and optional targetYear.
     * - If no results, retries without year.
     * - If still no results and title has punctuation/subtitles (':', '-'), searches prefix.
     */
    suspend fun searchMovieSmart(
        apiService: TmdbApiService,
        apiKey: String,
        rawTitle: String,
        targetYear: String? = null
    ): TmdbSearchResult? {
        val cleanTitle = MediaTitleSanitizer.cleanCandidateTitle(rawTitle)
        if (cleanTitle.isBlank() || MediaTitleSanitizer.isNonMovieEpisode(cleanTitle)) {
            return null
        }

        val cleanYear = targetYear?.trim()?.take(4)?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

        // Attempt 1: Search with year if available
        if (cleanYear != null) {
            try {
                val resp = apiService.searchMovie(apiKey, cleanTitle, cleanYear)
                val match = findBestMovieMatch(resp.results, cleanTitle, cleanYear)
                if (match != null) return match
            } catch (e: Exception) {
                Log.w(TAG, "Search with year failed for '$cleanTitle ($cleanYear)': ${e.message}")
            }
        }

        // Attempt 2: Search by cleanTitle without year
        try {
            val resp = apiService.searchMovie(apiKey, cleanTitle, null)
            val match = findBestMovieMatch(resp.results, cleanTitle, cleanYear)
            if (match != null) return match
        } catch (e: Exception) {
            Log.w(TAG, "Search without year failed for '$cleanTitle': ${e.message}")
        }

        // Attempt 3: If title has ':' or ' - ', try search with main prefix
        val prefix = when {
            cleanTitle.contains(":") -> cleanTitle.substringBefore(":").trim()
            cleanTitle.contains(" - ") -> cleanTitle.substringBefore(" - ").trim()
            else -> null
        }
        if (prefix != null && prefix.length >= 3 && prefix != cleanTitle) {
            try {
                val resp = apiService.searchMovie(apiKey, prefix, cleanYear)
                val match = findBestMovieMatch(resp.results, cleanTitle, cleanYear)
                if (match != null) return match
            } catch (e: Exception) {
                Log.w(TAG, "Search with prefix failed for '$prefix': ${e.message}")
            }
        }

        return null
    }

    /**
     * Executes smart TMDB TV search with fallbacks.
     */
    suspend fun searchTvSmart(
        apiService: TmdbApiService,
        apiKey: String,
        rawTitle: String,
        targetYear: String? = null
    ): TmdbTvSearchResult? {
        val cleanTitle = MediaTitleSanitizer.cleanCandidateTitle(rawTitle)
        if (cleanTitle.isBlank() || MediaTitleSanitizer.isNonMovieEpisode(cleanTitle)) {
            return null
        }

        val cleanYear = targetYear?.trim()?.take(4)?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

        try {
            val resp = apiService.searchTv(apiKey, cleanTitle, cleanYear)
            val match = findBestTvMatch(resp.results, cleanTitle, cleanYear)
            if (match != null) return match
        } catch (e: Exception) {
            Log.w(TAG, "TV search failed for '$cleanTitle': ${e.message}")
        }

        if (cleanYear != null) {
            try {
                val resp = apiService.searchTv(apiKey, cleanTitle, null)
                val match = findBestTvMatch(resp.results, cleanTitle, cleanYear)
                if (match != null) return match
            } catch (e: Exception) {
                Log.w(TAG, "TV search retry failed for '$cleanTitle': ${e.message}")
            }
        }

        return null
    }
}
