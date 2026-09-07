package com.example

import com.example.data.local.MediaDao
import com.example.data.local.ProviderUsageStats
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.remote.LetterboxdCsvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class MediaItemUnitTest {

    @Test
    fun providersList_null_returnsEmpty() {
        val item = MediaItem(title = "Test Movie", providerIds = null)
        assertTrue(item.providersList.isEmpty())
    }

    @Test
    fun providersList_none_returnsEmpty() {
        val item = MediaItem(title = "Test Movie", providerIds = "none")
        assertTrue(item.providersList.isEmpty())
    }

    @Test
    fun providersList_commaSeparated_returnsCleanList() {
        val item = MediaItem(title = "Test Movie", providerIds = "netflix, hulu, max")
        assertEquals(listOf("netflix", "hulu", "max"), item.providersList)
    }

    @Test
    fun providersList_mixedWithNoneSentinel_filtersNoneOut() {
        val item = MediaItem(title = "Test Movie", providerIds = "netflix, none, prime, none")
        assertEquals(listOf("netflix", "prime"), item.providersList)
    }

    @Test
    fun providersList_emptyOrWhitespaceOnly_returnsEmpty() {
        val item = MediaItem(title = "Test Movie", providerIds = "  , ,   ")
        assertTrue(item.providersList.isEmpty())
    }

    @Test
    fun mediaStatus_enumSanityCheck() {
        assertEquals("PENDING_METADATA", MediaStatus.PENDING_METADATA.name)
        assertEquals("WATCHLIST", MediaStatus.WATCHLIST.name)
        assertEquals("INTENDING_TO_WATCH", MediaStatus.INTENDING_TO_WATCH.name)
        assertEquals("WATCHED", MediaStatus.WATCHED.name)
    }

    @Test
    fun providerUsageStats_costPerHour_calculatesCorrectly() {
        // 120 minutes = 2 hours, $10/mo = $5/hr
        val stats = ProviderUsageStats(
            providerId = "netflix",
            providerName = "Netflix",
            costPerMonth = 10.0,
            isActive = true,
            totalMinutes = 120
        )
        assertEquals(2.0, stats.totalHours, 0.001)
        assertEquals(5.0, stats.costPerHour, 0.001)
    }

    @Test
    fun providerUsageStats_zeroMinutes_returnsCostPerMonth() {
        val stats = ProviderUsageStats(
            providerId = "hulu",
            providerName = "Hulu",
            costPerMonth = 15.0,
            isActive = true,
            totalMinutes = 0
        )
        assertEquals(0.0, stats.totalHours, 0.001)
        assertEquals(15.0, stats.costPerHour, 0.001)
    }

    @Test
    fun letterboxdExportCsv_formatsCorrectly() {
        // Create dummy MediaDao proxy for parser instantiation
        val dummyDao = Proxy.newProxyInstance(
            MediaDao::class.java.classLoader,
            arrayOf(MediaDao::class.java)
        ) { _, _, _ -> null } as MediaDao

        val parser = LetterboxdCsvParser(dummyDao)
        val items = listOf(
            MediaItem(
                title = "Inception",
                rating = 9.0, // 9.0 out of 10
                status = MediaStatus.WATCHED.name,
                watchedAt = 1672531199000L // 2022-12-31 approx
            )
        )

        val csv = parser.generateLetterboxdExportCsv(items)
        assertTrue(csv.startsWith("Title,Year,Rating10,WatchedDate\n"))
        assertTrue(csv.contains("\"Inception\""))
        assertTrue(csv.contains("9.0"))
    }

    @Test
    fun watchlistVolume_perProvider_countsCorrectly() {
        val items = listOf(
            MediaItem(title = "Movie 1", providerIds = "netflix, hulu"),
            MediaItem(title = "Movie 2", providerIds = "netflix, prime"),
            MediaItem(title = "Movie 3", providerIds = "max"),
            MediaItem(title = "Movie 4", providerIds = "none")
        )
        val netflixCount = items.count { it.providersList.any { p -> p.equals("netflix", ignoreCase = true) } }
        val huluCount = items.count { it.providersList.any { p -> p.equals("hulu", ignoreCase = true) } }
        val maxCount = items.count { it.providersList.any { p -> p.equals("max", ignoreCase = true) } }
        val appleCount = items.count { it.providersList.any { p -> p.equals("apple", ignoreCase = true) } }

        assertEquals(2, netflixCount)
        assertEquals(1, huluCount)
        assertEquals(1, maxCount)
        assertEquals(0, appleCount)
    }
}
