package com.example

import com.example.data.model.MediaItem
import com.example.data.model.StreamingProvider
import com.example.data.util.WatchedAnalyticsCalculator
import org.junit.Assert.*
import org.junit.Test

class WatchedAnalyticsCalculatorUnitTest {

    @Test
    fun testExtractReleaseYearFromTitle() {
        val item1 = MediaItem(title = "The Abyss (1989)")
        assertEquals(1989, WatchedAnalyticsCalculator.extractReleaseYear(item1))

        val item2 = MediaItem(title = "Top Gun: Maverick 2022")
        assertEquals(2022, WatchedAnalyticsCalculator.extractReleaseYear(item2))

        val item3 = MediaItem(title = "Alien", overview = "Imported from Letterboxd diary: Alien (1979). Logged on 2024-01-01.")
        assertEquals(1979, WatchedAnalyticsCalculator.extractReleaseYear(item3))
    }

    @Test
    fun testDetermineEra() {
        assertEquals("2020s", WatchedAnalyticsCalculator.determineEra(2023))
        assertEquals("2010s", WatchedAnalyticsCalculator.determineEra(2015))
        assertEquals("2000s", WatchedAnalyticsCalculator.determineEra(2004))
        assertEquals("1990s", WatchedAnalyticsCalculator.determineEra(1994))
        assertEquals("1980s", WatchedAnalyticsCalculator.determineEra(1985))
        assertEquals("1970s", WatchedAnalyticsCalculator.determineEra(1977))
        assertEquals("Pre-1970s", WatchedAnalyticsCalculator.determineEra(1968))
        assertEquals("Unknown", WatchedAnalyticsCalculator.determineEra(null))
    }

    @Test
    fun testCalculateAnalytics() {
        val providers = listOf(
            StreamingProvider(id = "netflix", name = "Netflix", costPerMonth = 15.49, isActive = true),
            StreamingProvider(id = "hulu", name = "Hulu", costPerMonth = 7.99, isActive = true)
        )

        val items = listOf(
            MediaItem(title = "Inception (2010)", rating = 9.0, genres = "Action, Sci-Fi", providerIds = "netflix"),
            MediaItem(title = "Interstellar (2014)", rating = 8.5, genres = "Sci-Fi, Drama", providerIds = "netflix,hulu"),
            MediaItem(title = "The Dark Knight (2008)", rating = 9.5, genres = "Action, Crime", providerIds = "hulu"),
            MediaItem(title = "Pulp Fiction (1994)", rating = 8.8, genres = "Crime, Drama", providerIds = null)
        )

        val analytics = WatchedAnalyticsCalculator.calculateAnalytics(items, providers)

        assertEquals(4, analytics.totalFilms)
        assertEquals(4, analytics.ratedCount)
        assertTrue(analytics.avgRating > 8.5)
        assertEquals("2010s", analytics.topEra)

        // Verify Era Breakdown
        val eras = analytics.eraBreakdown.map { it.eraLabel }
        assertTrue(eras.contains("2010s"))
        assertTrue(eras.contains("2000s"))
        assertTrue(eras.contains("1990s"))

        // Verify Genre Breakdown
        val topGenres = analytics.genreBreakdown.map { it.genre }
        assertTrue(topGenres.contains("Sci-Fi"))
        assertTrue(topGenres.contains("Action"))

        // Verify Service Breakdown
        val services = analytics.serviceBreakdown.map { it.displayName }
        assertTrue(services.contains("Netflix"))
        assertTrue(services.contains("Hulu"))
        assertTrue(services.contains("Other / Theatrical / Physical"))
    }
}
