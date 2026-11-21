package com.example

import com.example.data.local.MediaDao
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.remote.LetterboxdCsvParser
import com.example.data.remote.LetterboxdSyncManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class LetterboxdCsvParserUnitTest {

    private lateinit var parser: LetterboxdCsvParser

    @Before
    fun setUp() {
        val dummyDao = Proxy.newProxyInstance(
            MediaDao::class.java.classLoader,
            arrayOf(MediaDao::class.java)
        ) { _, _, _ -> null } as MediaDao
        parser = LetterboxdCsvParser(dummyDao)
    }

    @Test
    fun parseCsvLine_simpleTokens_parsedCorrectly() {
        val line = "Title,Year,Rating10,WatchedDate"
        val tokens = parser.parseCsvLine(line)
        assertEquals(listOf("Title", "Year", "Rating10", "WatchedDate"), tokens)
    }

    @Test
    fun parseCsvLine_commasInsideQuotes_preservedIntact() {
        val line = "\"The Lord of the Rings: The Fellowship of the Ring, Extended Edition\",2001,5,2026-09-01"
        val tokens = parser.parseCsvLine(line)
        assertEquals(4, tokens.size)
        assertEquals("The Lord of the Rings: The Fellowship of the Ring, Extended Edition", tokens[0])
        assertEquals("2001", tokens[1])
        assertEquals("5", tokens[2])
        assertEquals("2026-09-01", tokens[3])
    }

    @Test
    fun parseCsvLine_escapedDoubleQuotes_unEscapedCorrectly() {
        val line = "\"She\"\"s the One\",1996,4,2026-09-02"
        val tokens = parser.parseCsvLine(line)
        assertEquals(4, tokens.size)
        assertEquals("She\"s the One", tokens[0])
        assertEquals("1996", tokens[1])
    }

    @Test
    fun parseCsvLine_emptyTokens_parsedAsEmptyStrings() {
        val line = "Movie,,,"
        val tokens = parser.parseCsvLine(line)
        assertEquals(listOf("Movie", "", "", ""), tokens)
    }

    @Test
    fun generateLetterboxdExportCsv_escapesQuotesAndFormatsOutput() {
        val items = listOf(
            MediaItem(
                title = "He said \"Hello\"",
                rating = 8.5,
                status = MediaStatus.WATCHED.name,
                watchedAt = 1700000000000L
            )
        )
        val csv = parser.generateLetterboxdExportCsv(items)
        val lines = csv.trim().lines()

        assertEquals("Title,Year,Rating10,WatchedDate", lines[0])
        assertTrue(lines[1].startsWith("\"He said \"\"Hello\"\"\""))
        assertTrue(lines[1].contains("8.5"))
    }

    @Test
    fun parseRssXml_extractsTmdbIdPosterAndUnescapesEntities() {
        val dummyDao = Proxy.newProxyInstance(
            MediaDao::class.java.classLoader,
            arrayOf(MediaDao::class.java)
        ) { _, _, _ -> null } as MediaDao
        val manager = LetterboxdSyncManager(dummyDao)

        val rssXml = """
            <?xml version='1.0' encoding='utf-8'?>
            <rss version="2.0" xmlns:letterboxd="https://letterboxd.com" xmlns:tmdb="https://themoviedb.org">
              <channel>
                <item>
                  <title>The Dead Don&#039;t Die, 2019</title>
                  <link>https://letterboxd.com/scriptedmind/film/the-dead-dont-die-2019/</link>
                  <pubDate>Thu, 19 Nov 2026 14:04:13 +1300</pubDate>
                  <letterboxd:watchedDate>2026-11-18</letterboxd:watchedDate>
                  <letterboxd:rewatch>No</letterboxd:rewatch>
                  <letterboxd:filmTitle>The Dead Don&#039;t Die</letterboxd:filmTitle>
                  <letterboxd:filmYear>2019</letterboxd:filmYear>
                  <letterboxd:memberRating>4.0</letterboxd:memberRating>
                  <tmdb:movieId>535581</tmdb:movieId>
                  <description><![CDATA[ <p><img src="https://a.ltrbxd.com/resized/film-poster/4/6/4/7/3/5/464735-poster.jpg"/></p> <p>Watched on Wednesday November 18, 2026.</p> ]]></description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val entries = manager.parseRssXml(rssXml)
        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("The Dead Don't Die", entry.title)
        assertEquals("2019", entry.year)
        assertEquals("535581", entry.tmdbId)
        assertEquals("https://a.ltrbxd.com/resized/film-poster/4/6/4/7/3/5/464735-poster.jpg", entry.posterUrl)
        assertEquals(8.0, entry.ratingTenScale!!, 0.01)
        assertEquals("2026-11-18", entry.dateString)
        assertNull(entry.notes)
    }
}
