package com.example.data.remote

import android.util.Log
import android.util.Xml
import com.example.data.local.MediaDao
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

data class LetterboxdSyncResult(
    val isSuccess: Boolean,
    val username: String,
    val totalFetched: Int,
    val newlyImportedCount: Int,
    val newlyImportedItems: List<MediaItem>,
    val errorMessage: String? = null
)

class LetterboxdSyncManager(private val mediaDao: MediaDao) {

    val csvParser = LetterboxdCsvParser(mediaDao)

    companion object {
        private const val TAG = "LetterboxdSync"
    }

    suspend fun importFileStream(inputStream: InputStream, filename: String): LetterboxdFileImportResult =
        csvParser.parseAndImportStream(inputStream, filename)

    fun generateLetterboxdExportCsv(watchedItems: List<MediaItem>): String =
        csvParser.generateLetterboxdExportCsv(watchedItems)

    suspend fun syncUserDiary(username: String): LetterboxdSyncResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().lowercase()
        if (cleanUser.isBlank()) {
            return@withContext LetterboxdSyncResult(
                isSuccess = false,
                username = cleanUser,
                totalFetched = 0,
                newlyImportedCount = 0,
                newlyImportedItems = emptyList(),
                errorMessage = "Letterboxd username cannot be blank."
            )
        }

        val rssUrl = "https://letterboxd.com/$cleanUser/rss/"
        Log.d(TAG, "Fetching live Letterboxd RSS from: $rssUrl")

        try {
            val url = URL(rssUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("User-Agent", "Streamwise-Android/1.4")
                setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml")
            }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                val errorMsg = if (responseCode == 404) {
                    "Letterboxd user '$cleanUser' not found (404). Check spelling."
                } else {
                    "HTTP Error $responseCode fetching RSS feed."
                }
                return@withContext LetterboxdSyncResult(
                    isSuccess = false,
                    username = cleanUser,
                    totalFetched = 0,
                    newlyImportedCount = 0,
                    newlyImportedItems = emptyList(),
                    errorMessage = errorMsg
                )
            }

            val xmlContent = conn.inputStream.bufferedReader().use { it.readText() }
            val parsedEntries = parseRssXml(xmlContent)
            Log.d(TAG, "Successfully parsed ${parsedEntries.size} items from Letterboxd RSS.")

            val existingItems = mediaDao.getAllMediaItemsList().associateBy { it.title.trim().lowercase() }.toMutableMap()
            val newlyImported = mutableListOf<MediaItem>()

            for (entry in parsedEntries) {
                val normalizedTitle = entry.title.trim().lowercase()
                if (normalizedTitle.isEmpty()) continue

                val existing = existingItems[normalizedTitle]
                if (existing != null) {
                    val hasNewerWatch = entry.watchedAt > (existing.watchedAt ?: 0L)
                    val hasNewRating = entry.ratingTenScale != null && existing.rating == null
                    val hasNewNotes = !entry.notes.isNullOrBlank() && existing.userNotes.isNullOrBlank()
                    val needsStatusUpgrade = existing.status != MediaStatus.WATCHED.name

                    if (needsStatusUpgrade || hasNewerWatch || hasNewRating || hasNewNotes) {
                        val updated = existing.copy(
                            status = MediaStatus.WATCHED.name,
                            watchedAt = if (hasNewerWatch || existing.watchedAt == null) entry.watchedAt else existing.watchedAt,
                            rating = entry.ratingTenScale ?: existing.rating,
                            userNotes = entry.notes ?: existing.userNotes,
                            importSource = "Letterboxd Live RSS (@$cleanUser)",
                            releaseDate = existing.releaseDate ?: entry.year?.takeIf { it.isNotBlank() },
                            overview = existing.overview ?: "Imported from Letterboxd diary: ${entry.title} (${entry.year ?: "N/A"}). Logged on ${entry.dateString}."
                        )
                        mediaDao.updateMediaItem(updated)
                        existingItems[normalizedTitle] = updated
                        newlyImported.add(updated)
                    }
                } else {
                    val item = MediaItem(
                        title = entry.title,
                        sharedUrl = entry.link,
                        status = MediaStatus.WATCHED.name,
                        addedAt = entry.watchedAt,
                        watchedAt = entry.watchedAt,
                        rating = entry.ratingTenScale,
                        userNotes = entry.notes,
                        importSource = "Letterboxd Live RSS (@$cleanUser)",
                        releaseDate = entry.year?.takeIf { it.isNotBlank() },
                        overview = "Imported from Letterboxd diary: ${entry.title} (${entry.year ?: "N/A"}). Logged on ${entry.dateString}."
                    )
                    val insertedId = mediaDao.insertMediaItem(item)
                    val insertedItem = item.copy(id = insertedId)
                    existingItems[normalizedTitle] = insertedItem
                    newlyImported.add(insertedItem)
                }
            }

            Log.d(TAG, "Imported ${newlyImported.size} new watched titles into Room DB.")
            return@withContext LetterboxdSyncResult(
                isSuccess = true,
                username = cleanUser,
                totalFetched = parsedEntries.size,
                newlyImportedCount = newlyImported.size,
                newlyImportedItems = newlyImported
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error syncing Letterboxd RSS: ${e.message}", e)
            return@withContext LetterboxdSyncResult(
                isSuccess = false,
                username = cleanUser,
                totalFetched = 0,
                newlyImportedCount = 0,
                newlyImportedItems = emptyList(),
                errorMessage = e.localizedMessage ?: "Unknown network error during Letterboxd sync."
            )
        }
    }

    private data class ParsedRssEntry(
        val title: String,
        val year: String?,
        val link: String?,
        val dateString: String,
        val watchedAt: Long,
        val ratingTenScale: Double?,
        val notes: String?
    )

    private fun parseRssXml(xml: String): List<ParsedRssEntry> {
        val entries = mutableListOf<ParsedRssEntry>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var inItem = false

        var curTitle: String? = null
        var curFilmTitle: String? = null
        var curFilmYear: String? = null
        var curLink: String? = null
        var curWatchedDate: String? = null
        var curRating: String? = null
        var curPubDate: String? = null
        var curDescription: String? = null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val rfcDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        inItem = true
                        curTitle = null
                        curFilmTitle = null
                        curFilmYear = null
                        curLink = null
                        curWatchedDate = null
                        curRating = null
                        curPubDate = null
                        curDescription = null
                    } else if (inItem) {
                        val cleanTag = tagName.substringAfterLast(':').lowercase()
                        when (cleanTag) {
                            "title" -> curTitle = parser.nextText().trim()
                            "link" -> curLink = parser.nextText().trim()
                            "filmtitle" -> curFilmTitle = parser.nextText().trim()
                            "filmyear" -> curFilmYear = parser.nextText().trim()
                            "watcheddate" -> curWatchedDate = parser.nextText().trim()
                            "memberrating" -> curRating = parser.nextText().trim()
                            "pubdate" -> curPubDate = parser.nextText().trim()
                            "description" -> curDescription = parser.nextText().trim()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("item", ignoreCase = true) && inItem) {
                        val finalTitle = curFilmTitle ?: extractTitleFromRssTitle(curTitle)
                        if (!finalTitle.isNullOrBlank()) {
                            val watchDateTimestamp = try {
                                if (!curWatchedDate.isNullOrBlank()) {
                                    dateFormat.parse(curWatchedDate)?.time ?: System.currentTimeMillis()
                                } else if (!curPubDate.isNullOrBlank()) {
                                    rfcDateFormat.parse(curPubDate)?.time ?: System.currentTimeMillis()
                                } else {
                                    System.currentTimeMillis()
                                }
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }

                            val ratingTen = curRating?.toDoubleOrNull()?.let { it * 2.0 } // 5-star to 10-scale
                            val cleanNotes = cleanHtmlDescription(curDescription)

                            entries.add(
                                ParsedRssEntry(
                                    title = finalTitle,
                                    year = curFilmYear,
                                    link = curLink,
                                    dateString = curWatchedDate ?: curPubDate ?: "Recent",
                                    watchedAt = watchDateTimestamp,
                                    ratingTenScale = ratingTen,
                                    notes = cleanNotes
                                )
                            )
                        }
                        inItem = false
                    }
                }
            }
            eventType = parser.next()
        }
        return entries
    }

    private fun extractTitleFromRssTitle(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        // Letterboxd RSS titles are typically: "Movie Name, 2024 - ★★★★" or "Movie Name, 2024"
        val commaIndex = raw.lastIndexOf(',')
        return if (commaIndex > 0) {
            raw.substring(0, commaIndex).trim()
        } else {
            val dashIndex = raw.indexOf(" - ")
            if (dashIndex > 0) raw.substring(0, dashIndex).trim() else raw.trim()
        }
    }

    private fun cleanHtmlDescription(html: String?): String? {
        if (html.isNullOrBlank()) return null
        // Strip out <p><img ...></p> poster markup and retain text review paragraphs
        val stripped = html.replace(Regex("<img[^>]*>"), "")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (stripped.isNotBlank()) stripped else null
    }
}
