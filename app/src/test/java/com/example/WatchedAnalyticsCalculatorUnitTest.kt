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

        val item4 = MediaItem(title = "Shawshank Redemption", releaseDate = "1994-10-14")
        assertEquals(1994, WatchedAnalyticsCalculator.extractReleaseYear(item4))

        val item5 = MediaItem(title = "The Babysitter", sharedUrl = "https://letterboxd.com/scriptedmind/film/the-babysitter-2017/")
        assertEquals(2017, WatchedAnalyticsCalculator.extractReleaseYear(item5))

        val item6 = MediaItem(title = "About Last Night", userNotes = "Podcast: The Rewatchables (1986)")
        assertEquals(1986, WatchedAnalyticsCalculator.extractReleaseYear(item6))
    }

    @Test
    fun testExtractRating() {
        val item1 = MediaItem(title = "Direct Rating", rating = 8.5)
        assertEquals(8.5, WatchedAnalyticsCalculator.extractRating(item1)!!, 0.01)

        val item2 = MediaItem(title = "Star Glyphs", userNotes = "Loved this! ★★★★½ masterpiece.")
        assertEquals(9.0, WatchedAnalyticsCalculator.extractRating(item2)!!, 0.01)

        val item3 = MediaItem(title = "Three Stars", overview = "Letterboxd review: ★★★")
        assertEquals(6.0, WatchedAnalyticsCalculator.extractRating(item3)!!, 0.01)

        val item4 = MediaItem(title = "Slash 10", userNotes = "Rating: 7.5/10 solid film")
        assertEquals(7.5, WatchedAnalyticsCalculator.extractRating(item4)!!, 0.01)

        val item5 = MediaItem(title = "Slash 5", userNotes = "Rating: 4/5")
        assertEquals(8.0, WatchedAnalyticsCalculator.extractRating(item5)!!, 0.01)

        val item6 = MediaItem(title = "No Rating")
        assertNull(WatchedAnalyticsCalculator.extractRating(item6))
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

    @Test
    fun testNormalizeTitle() {
        assertEquals("the dark knight", WatchedAnalyticsCalculator.normalizeTitle("The Dark Knight (2008)"))
        assertEquals("the abyss", WatchedAnalyticsCalculator.normalizeTitle("The Abyss (Special Edition)"))
        assertEquals("fargo", WatchedAnalyticsCalculator.normalizeTitle("  Fargo (1996)  "))
    }

    @Test
    fun testAuteursAndActorsAndBlindSpots() {
        val watched = listOf(
            MediaItem(title = "Inception (2010)", rating = 9.0, genres = "Action, Sci-Fi"),
            MediaItem(title = "Interstellar (2014)", rating = 8.5, genres = "Sci-Fi, Drama"),
            MediaItem(title = "Oppenheimer (2023)", rating = 9.0, genres = "Biography, Drama"),
            MediaItem(title = "The Dark Knight (2008)", rating = 9.5, genres = "Action, Crime"),
            MediaItem(title = "Pulp Fiction (1994)", rating = 9.0, genres = "Crime, Drama"),
            MediaItem(title = "Kill Bill: Vol. 1 (2003)", rating = 8.0, genres = "Action, Thriller"),
            MediaItem(title = "Inglourious Basterds (2009)", rating = 8.5, genres = "Adventure, Drama"),
            MediaItem(title = "Django Unchained (2012)", rating = 8.5, genres = "Drama, Western"),
            MediaItem(title = "Once Upon a Time in Hollywood (2019)", rating = 8.0, genres = "Comedy, Drama"),
            MediaItem(title = "Top Gun (1986)", rating = 8.0, genres = "Action"),
            MediaItem(title = "Top Gun: Maverick (2022)", rating = 8.5, genres = "Action")
        )

        val watchlist = listOf(
            MediaItem(title = "The Searchers (1956)", genres = "Western"),
            MediaItem(title = "Free Solo (2018)", genres = "Documentary"),
            MediaItem(title = "12 Angry Men (1957)", genres = "Drama"),
            MediaItem(title = "The Godfather (1972)", genres = "Crime, Drama")
        )

        val analytics = WatchedAnalyticsCalculator.calculateAnalytics(
            items = watched,
            watchlistItems = watchlist
        )

        // Verify Directors
        assertTrue(analytics.topDirectors.isNotEmpty())
        val nolan = analytics.topDirectors.find { it.name == "Christopher Nolan" }
        assertNotNull("Nolan should be found", nolan)
        assertEquals(4, nolan!!.count)
        assertTrue(nolan.avgRating != null && nolan.avgRating!! > 8.0)

        val tarantino = analytics.topDirectors.find { it.name == "Quentin Tarantino" }
        assertNotNull("Tarantino should be found", tarantino)
        assertEquals(5, tarantino!!.count)

        // Verify Actors
        assertTrue(analytics.topActors.isNotEmpty())
        val cruise = analytics.topActors.find { it.name == "Tom Cruise" }
        assertNotNull("Tom Cruise should be found", cruise)
        assertEquals(2, cruise!!.count)

        val dicaprio = analytics.topActors.find { it.name == "Leonardo DiCaprio" }
        assertNotNull("Leonardo DiCaprio should be found", dicaprio)
        assertEquals(3, dicaprio!!.count)

        // Verify Blind Spots
        assertTrue(analytics.blindSpots.isNotEmpty())
        val pre1970s = analytics.blindSpots.find { it.title.contains("Pre-1970s") }
        assertNotNull("Pre-1970s blind spot should be identified", pre1970s)
        assertTrue(pre1970s!!.sampleWatchlistTitles.any { it.contains("The Searchers") || it.contains("12 Angry Men") })

        val docs = analytics.blindSpots.find { it.title.contains("Documentaries") }
        assertNotNull("Documentary blind spot should be identified", docs)
        assertTrue(docs!!.sampleWatchlistTitles.contains("Free Solo (2018)"))
    }
}
