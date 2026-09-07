package com.example

import com.example.data.util.MediaTitleSanitizer
import org.junit.Assert.*
import org.junit.Test

class MediaTitleSanitizerUnitTest {

    @Test
    fun testDetectsNonMovieEpisodes() {
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("A Rewatchables Summer Mailbag!"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("The 1976 Movie Draft"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("The 2026 Movie Auction Returns!"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("The Diane Keaton Hall of Fame"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("Ask Sean Anything"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("We Went to Cannes! These Are the 10 Best"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("Final 2026 Oscar Predictions"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("The 900th-Episode Mega-Voicemailbag!"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("The Advice Hour"))
        assertTrue(MediaTitleSanitizer.isNonMovieEpisode("Matinee Monday: Mini-Episode"))
    }

    @Test
    fun testValidMoviesPassNonMovieCheck() {
        assertFalse(MediaTitleSanitizer.isNonMovieEpisode("Heat"))
        assertFalse(MediaTitleSanitizer.isNonMovieEpisode("The Abyss"))
        assertFalse(MediaTitleSanitizer.isNonMovieEpisode("The Departed"))
        assertFalse(MediaTitleSanitizer.isNonMovieEpisode("Waterworld"))
        assertFalse(MediaTitleSanitizer.isNonMovieEpisode("A Few Good Men"))
        assertFalse(MediaTitleSanitizer.isNonMovieEpisode("Oppenheimer"))
        assertFalse(MediaTitleSanitizer.isNonMovieEpisode("Dune: Part Two"))
    }

    @Test
    fun testCleanCandidateTitle() {
        assertEquals("Heat", MediaTitleSanitizer.cleanCandidateTitle("The Re-Heat"))
        assertEquals("Departed", MediaTitleSanitizer.cleanCandidateTitle("The Re-Departed"))
        assertEquals("A Few Good Men", MediaTitleSanitizer.cleanCandidateTitle("A Few Good (Re)Men"))
        assertEquals("Miami Vice: Calderone's Return", MediaTitleSanitizer.cleanCandidateTitle("Miami Vice: Calderone's Return (Part 1 + 2)"))
        assertEquals("Civil War", MediaTitleSanitizer.cleanCandidateTitle("‘Civil War’ and Alex Garland. Plus: The Top Five Movies"))
        assertEquals("Challengers", MediaTitleSanitizer.cleanCandidateTitle("‘Challengers’ and the Return of the Movie Star"))
        assertEquals("Face/Off", MediaTitleSanitizer.cleanCandidateTitle("Ep. 45: Face/Off (with Paul Scheer)"))
    }
}
