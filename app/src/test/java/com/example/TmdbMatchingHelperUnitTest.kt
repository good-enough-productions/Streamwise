package com.example

import com.example.data.remote.TmdbMatchingHelper
import com.example.data.remote.TmdbSearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TmdbMatchingHelperUnitTest {

    @Test
    fun testNormalizeTitle() {
        assertEquals("socialnetwork", TmdbMatchingHelper.normalizeTitle("The Social Network"))
        assertEquals("darkknight", TmdbMatchingHelper.normalizeTitle("A Dark Knight"))
        assertEquals("americanwerewolfinlondon", TmdbMatchingHelper.normalizeTitle("An American Werewolf in London"))
        assertEquals("killbillvol1", TmdbMatchingHelper.normalizeTitle("Kill Bill: Vol. 1"))
        assertEquals("faceoff", TmdbMatchingHelper.normalizeTitle("Face/Off"))
        assertEquals("spiderman", TmdbMatchingHelper.normalizeTitle("Spider-Man!"))
    }

    @Test
    fun testExactMatchBeatsPopularityBias() {
        // "Network" exact match (id: 10774) vs "The Social Network" (id: 37799, higher popularity)
        val candidate1 = TmdbSearchResult(
            id = 37799,
            title = "The Social Network",
            releaseDate = "2010-09-24",
            overview = "Mark Zuckerberg creates Facebook.",
            posterPath = "/social.jpg",
            voteAverage = 7.4
        )
        val candidate2 = TmdbSearchResult(
            id = 10774,
            title = "Network",
            releaseDate = "1976-11-27",
            overview = "Howard Beale mad as hell.",
            posterPath = "/network.jpg",
            voteAverage = 7.9
        )

        // Given search results ordered by popularity: [The Social Network, Network]
        val results = listOf(candidate1, candidate2)

        val bestMatch = TmdbMatchingHelper.findBestMovieMatch(
            results = results,
            queryTitle = "Network",
            targetYear = "1976"
        )

        assertNotNull(bestMatch)
        assertEquals(10774, bestMatch!!.id)
        assertEquals("Network", bestMatch.title)
    }

    @Test
    fun testNormalizedMatch() {
        val candidate = TmdbSearchResult(
            id = 24,
            title = "Kill Bill: Vol. 1",
            releaseDate = "2003-10-10",
            overview = "The Bride begins quest for revenge.",
            posterPath = null,
            voteAverage = 8.2
        )

        val bestMatch = TmdbMatchingHelper.findBestMovieMatch(
            results = listOf(candidate),
            queryTitle = "Kill Bill Vol 1",
            targetYear = null
        )

        assertNotNull(bestMatch)
        assertEquals(24, bestMatch!!.id)
    }

    @Test
    fun testYearDisambiguation() {
        val candidate1984 = TmdbSearchResult(
            id = 9331,
            title = "Dune",
            releaseDate = "1984-12-14",
            overview = "David Lynch adaptation.",
            posterPath = null,
            voteAverage = 6.5
        )
        val candidate2021 = TmdbSearchResult(
            id = 438631,
            title = "Dune",
            releaseDate = "2021-09-15",
            overview = "Denis Villeneuve adaptation.",
            posterPath = null,
            voteAverage = 8.0
        )

        val bestMatch1984 = TmdbMatchingHelper.findBestMovieMatch(
            results = listOf(candidate2021, candidate1984),
            queryTitle = "Dune",
            targetYear = "1984"
        )

        assertNotNull(bestMatch1984)
        assertEquals(9331, bestMatch1984!!.id)

        val bestMatch2021 = TmdbMatchingHelper.findBestMovieMatch(
            results = listOf(candidate1984, candidate2021),
            queryTitle = "Dune",
            targetYear = "2021"
        )

        assertNotNull(bestMatch2021)
        assertEquals(438631, bestMatch2021!!.id)
    }
}
