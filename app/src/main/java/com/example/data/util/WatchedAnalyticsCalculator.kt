package com.example.data.util

import com.example.data.model.MediaItem
import com.example.data.model.StreamingProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class EraStat(
    val eraLabel: String,
    val count: Int,
    val percentage: Float
)

data class GenreStat(
    val genre: String,
    val count: Int,
    val percentage: Float,
    val avgRating: Double?
)

data class ServiceStat(
    val providerId: String,
    val displayName: String,
    val count: Int,
    val percentage: Float
)

data class RatingBinStat(
    val binLabel: String,
    val count: Int,
    val percentage: Float
)

data class WatchedAnalytics(
    val totalFilms: Int,
    val totalHours: Int,
    val avgRating: Double,
    val ratedCount: Int,
    val topEra: String?,
    val topGenre: String?,
    val eraBreakdown: List<EraStat>,
    val genreBreakdown: List<GenreStat>,
    val serviceBreakdown: List<ServiceStat>,
    val ratingBins: List<RatingBinStat>,
    val mostActiveMonth: String?,
    val mostActiveMonthCount: Int,
    val watchedThisYear: Int,
    val watchedLastYear: Int
)

object WatchedAnalyticsCalculator {

    private val YEAR_PAREN_REGEX = Regex("\\((19\\d\\d|20\\d\\d)\\)")
    private val YEAR_LETTERBOXD_REGEX = Regex("Letterboxd diary: .*?\\b(19\\d\\d|20\\d\\d)\\b", RegexOption.IGNORE_CASE)
    private val YEAR_WORD_REGEX = Regex("\\b(19\\d\\d|20\\d\\d)\\b")

    /**
     * Extracts the 4-digit release year from title, overview, or notes.
     */
    fun extractReleaseYear(item: MediaItem): Int? {
        // 1. Look for (YYYY) in title
        YEAR_PAREN_REGEX.find(item.title)?.let { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
        }

        // 2. Look for Letterboxd diary pattern in overview: "Movie (YYYY)"
        if (!item.overview.isNullOrBlank()) {
            YEAR_LETTERBOXD_REGEX.find(item.overview)?.let { match ->
                match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
            YEAR_PAREN_REGEX.find(item.overview)?.let { match ->
                match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
        }

        // 3. Look for standalone 4-digit year in title
        YEAR_WORD_REGEX.find(item.title)?.let { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
        }

        return null
    }

    /**
     * Maps a release year to a human-readable cinema era.
     */
    fun determineEra(year: Int?): String {
        if (year == null) return "Unknown"
        return when {
            year >= 2020 -> "2020s"
            year >= 2010 -> "2010s"
            year >= 2000 -> "2000s"
            year >= 1990 -> "1990s"
            year >= 1980 -> "1980s"
            year >= 1970 -> "1970s"
            else -> "Pre-1970s"
        }
    }

    /**
     * Generates complete cinephile analytics from a list of watched media items.
     */
    fun calculateAnalytics(
        items: List<MediaItem>,
        allProviders: List<StreamingProvider> = emptyList()
    ): WatchedAnalytics {
        val total = items.size
        if (total == 0) {
            return WatchedAnalytics(
                totalFilms = 0,
                totalHours = 0,
                avgRating = 0.0,
                ratedCount = 0,
                topEra = null,
                topGenre = null,
                eraBreakdown = emptyList(),
                genreBreakdown = emptyList(),
                serviceBreakdown = emptyList(),
                ratingBins = emptyList(),
                mostActiveMonth = null,
                mostActiveMonthCount = 0,
                watchedThisYear = 0,
                watchedLastYear = 0
            )
        }

        val totalHours = (total * 110) / 60
        val ratedItems = items.filter { (it.rating ?: 0.0) > 0.0 }
        val avgRating = if (ratedItems.isNotEmpty()) {
            ratedItems.map { it.rating!! }.average()
        } else {
            0.0
        }

        // --- 1. Era Breakdown ---
        val eraOrder = listOf("2020s", "2010s", "2000s", "1990s", "1980s", "1970s", "Pre-1970s", "Unknown")
        val eraCounts = items.groupBy { determineEra(extractReleaseYear(it)) }
            .mapValues { it.value.size }
        val eraBreakdown = eraOrder.mapNotNull { era ->
            val count = eraCounts[era] ?: 0
            if (count > 0) {
                EraStat(eraLabel = era, count = count, percentage = (count.toFloat() / total) * 100f)
            } else null
        }
        val topEra = eraBreakdown.filter { it.eraLabel != "Unknown" }.maxByOrNull { it.count }?.eraLabel

        // --- 2. Genre Breakdown & Avg Rating per Genre ---
        val genreItemsMap = mutableMapOf<String, MutableList<MediaItem>>()
        for (item in items) {
            val genres = item.genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            for (g in genres) {
                genreItemsMap.getOrPut(g) { mutableListOf() }.add(item)
            }
        }
        val genreBreakdown = genreItemsMap.map { (genre, gItems) ->
            val count = gItems.size
            val gRated = gItems.filter { (it.rating ?: 0.0) > 0.0 }
            val gAvg = if (gRated.isNotEmpty()) gRated.map { it.rating!! }.average() else null
            GenreStat(
                genre = genre,
                count = count,
                percentage = (count.toFloat() / total) * 100f,
                avgRating = gAvg
            )
        }.sortedByDescending { it.count }
        val topGenre = genreBreakdown.firstOrNull()?.genre

        // --- 3. Streaming Service / Provider Breakdown ---
        val providerLookup = allProviders.associateBy { it.id.lowercase().trim() }
        val serviceCounts = mutableMapOf<String, Int>()
        var unassignedServiceCount = 0

        for (item in items) {
            val pList = item.providersList
            if (pList.isEmpty()) {
                unassignedServiceCount++
            } else {
                for (pId in pList) {
                    val cleanId = pId.lowercase().trim()
                    serviceCounts[cleanId] = (serviceCounts[cleanId] ?: 0) + 1
                }
            }
        }

        val serviceBreakdown = serviceCounts.map { (pId, count) ->
            val displayName = providerLookup[pId]?.name ?: pId.replace('_', ' ').replaceFirstChar { it.uppercase() }
            ServiceStat(
                providerId = pId,
                displayName = displayName,
                count = count,
                percentage = (count.toFloat() / total) * 100f
            )
        }.sortedByDescending { it.count }.toMutableList()

        if (unassignedServiceCount > 0) {
            serviceBreakdown.add(
                ServiceStat(
                    providerId = "other",
                    displayName = "Other / Theatrical / Physical",
                    count = unassignedServiceCount,
                    percentage = (unassignedServiceCount.toFloat() / total) * 100f
                )
            )
        }

        // --- 4. Rating Bins ---
        val binDefinitions = listOf(
            "9.0 - 10 ★" to (9.0..10.0),
            "7.0 - 8.9 ★" to (7.0..8.99),
            "5.0 - 6.9 ★" to (5.0..6.99),
            "3.0 - 4.9 ★" to (3.0..4.99),
            "0.5 - 2.9 ★" to (0.5..2.99)
        )
        val ratingBins = binDefinitions.map { (label, range) ->
            val count = ratedItems.count { (it.rating ?: 0.0) in range }
            RatingBinStat(
                binLabel = label,
                count = count,
                percentage = if (ratedItems.isNotEmpty()) (count.toFloat() / ratedItems.size) * 100f else 0f
            )
        }

        // --- 5. Viewing Rhythm & Calendar ---
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val lastYear = currentYear - 1
        val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        val monthCounts = mutableMapOf<String, Int>()
        var thisYearCount = 0
        var lastYearCount = 0

        for (item in items) {
            val ts = item.watchedAt ?: item.addedAt
            if (ts > 0L) {
                cal.timeInMillis = ts
                val y = cal.get(Calendar.YEAR)
                if (y == currentYear) thisYearCount++
                if (y == lastYear) lastYearCount++

                val monthStr = monthSdf.format(Date(ts))
                monthCounts[monthStr] = (monthCounts[monthStr] ?: 0) + 1
            }
        }

        val mostActive = monthCounts.maxByOrNull { it.value }

        return WatchedAnalytics(
            totalFilms = total,
            totalHours = totalHours,
            avgRating = avgRating,
            ratedCount = ratedItems.size,
            topEra = topEra,
            topGenre = topGenre,
            eraBreakdown = eraBreakdown,
            genreBreakdown = genreBreakdown,
            serviceBreakdown = serviceBreakdown,
            ratingBins = ratingBins,
            mostActiveMonth = mostActive?.key,
            mostActiveMonthCount = mostActive?.value ?: 0,
            watchedThisYear = thisYearCount,
            watchedLastYear = lastYearCount
        )
    }
}
