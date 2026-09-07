package com.example

import com.example.data.model.PodcastEpisodeCatalog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PodcastEpisodeCatalogUnitTest {

    @Before
    fun setUp() {
        PodcastEpisodeCatalog.clearForTesting()
    }

    @After
    fun tearDown() {
        PodcastEpisodeCatalog.clearForTesting()
    }

    @Test
    fun availablePodcasts_containsAllSixTargetShows() {
        val shows = PodcastEpisodeCatalog.AVAILABLE_PODCASTS
        val ids = shows.map { it.id }.toSet()

        assertTrue(ids.contains("the_rewatchables"))
        assertTrue(ids.contains("the_big_picture"))
        assertTrue(ids.contains("unspooled"))
        assertTrue(ids.contains("hdtgm"))
        assertTrue(ids.contains("what_went_wrong"))
        assertTrue(ids.contains("blank_check"))
        assertEquals(6, shows.size)

        val rewatchables = shows.first { it.id == "the_rewatchables" }
        assertEquals("The Rewatchables", rewatchables.name)
        assertEquals("🍿", rewatchables.emoji)

        val unspooled = shows.first { it.id == "unspooled" }
        assertEquals("Unspooled", unspooled.name)
        assertEquals("📽️", unspooled.emoji)
    }

    @Test
    fun normalize_handlesSpecialCharactersAndCasing() {
        assertEquals("heat", PodcastEpisodeCatalog.normalize("Heat"))
        assertEquals("shes the one", PodcastEpisodeCatalog.normalize("‘She’s the One’"))
        assertEquals("interview with the vampire", PodcastEpisodeCatalog.normalize("Interview with the Vampire!"))
        assertEquals("fast furious 6", PodcastEpisodeCatalog.normalize("Fast & Furious 6"))
    }

    @Test
    fun isCoveredOnPodcast_nullOrBlankPodcastId_returnsTrue() {
        assertTrue(PodcastEpisodeCatalog.isCoveredOnPodcast("Any Movie", null))
        assertTrue(PodcastEpisodeCatalog.isCoveredOnPodcast("Any Movie", ""))
        assertTrue(PodcastEpisodeCatalog.isCoveredOnPodcast("Any Movie", "   "))
    }

    @Test
    fun isCoveredOnPodcast_tagMatchingInImportSource_matchesCorrectly() {
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "City Slickers",
                podcastId = "the_rewatchables",
                importSource = "Podcast: The Rewatchables - City Slickers"
            )
        )
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Citizen Kane",
                podcastId = "unspooled",
                importSource = "Podcast: Unspooled - Citizen Kane"
            )
        )
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "The Room",
                podcastId = "hdtgm",
                importSource = "Podcast: How Did This Get Made? - The Room"
            )
        )
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "The Abyss",
                podcastId = "what_went_wrong",
                importSource = "Podcast: What Went Wrong - The Abyss"
            )
        )
    }

    @Test
    fun isCoveredOnPodcast_tagMatchingInNotes_matchesCorrectly() {
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Random Title",
                podcastId = "the_rewatchables",
                notes = "Discussed on [The Rewatchables]"
            )
        )
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Cult Movie",
                podcastId = "hdtgm",
                notes = "Covered on [HDTGM]"
            )
        )
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Classic Film",
                podcastId = "unspooled",
                notes = "Ranked on [Unspooled]"
            )
        )
    }

    @Test
    fun isCoveredOnPodcast_dynamicCatalog_matchesIndexedTitles() {
        PodcastEpisodeCatalog.loadCatalog(
            mapOf(
                "the_rewatchables" to setOf("city slickers", "jeremiah johnson", "us"),
                "unspooled" to setOf("singin in the rain", "casablanca")
            )
        )

        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "City Slickers",
                podcastId = "the_rewatchables"
            )
        )
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Casablanca",
                podcastId = "unspooled"
            )
        )
        // Movie in unspooled should not match rewatchables
        assertFalse(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Casablanca",
                podcastId = "the_rewatchables"
            )
        )
    }

    @Test
    fun isCoveredOnPodcast_fallbackMentions_matchesStaticList() {
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Waterworld",
                podcastId = "what_went_wrong"
            )
        )
        assertTrue(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "The Shining",
                podcastId = "what_went_wrong"
            )
        )
    }

    @Test
    fun isCoveredOnPodcast_unrelatedMovie_returnsFalse() {
        assertFalse(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Completely Unknown Indie Short 2026",
                podcastId = "the_rewatchables"
            )
        )
        assertFalse(
            PodcastEpisodeCatalog.isCoveredOnPodcast(
                movieTitle = "Unrelated Documentary",
                podcastId = "what_went_wrong"
            )
        )
    }
}
