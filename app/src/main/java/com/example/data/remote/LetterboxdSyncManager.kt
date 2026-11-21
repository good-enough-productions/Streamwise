package com.example.data.remote

import android.util.Log
import com.example.data.local.MediaDao
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.WatchSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

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

            // Build lookup map: normalized title -> best matching DB item.
            // When multiple DB items share a normalized title (e.g. re-watched films or
            // same title / different year), prefer the WATCHED entry so status-upgrade
            // logic fires correctly for the right row.
            val allDbItems = mediaDao.getAllMediaItemsList()
            val existingItems: MutableMap<String, MediaItem> = mutableMapOf()
            for (dbItem in allDbItems) {
                val key = TmdbMatchingHelper.normalizeTitle(dbItem.title)
                if (key.isBlank()) continue
                val prev = existingItems[key]
                // Prefer WATCHED over WATCHLIST; otherwise keep whichever was added more recently
                if (prev == null
                    || (prev.status != MediaStatus.WATCHED.name && dbItem.status == MediaStatus.WATCHED.name)
                    || (prev.status == dbItem.status && (dbItem.addedAt ?: 0L) > (prev.addedAt ?: 0L))
                ) {
                    existingItems[key] = dbItem
                }
                // Also index by "normalizedTitle|year" for year-aware disambiguation
                val yearKey = if (!dbItem.releaseDate.isNullOrBlank()) "$key|${dbItem.releaseDate!!.take(4)}" else null
                if (yearKey != null && !existingItems.containsKey(yearKey)) {
                    existingItems[yearKey] = dbItem
                }
            }
            val newlyImported = mutableListOf<MediaItem>()

            for (entry in parsedEntries) {
                val normalizedTitle = TmdbMatchingHelper.normalizeTitle(entry.title)
                if (normalizedTitle.isEmpty()) continue

                // Try year-aware match first (e.g. "cape fear|1991"), then title-only if years don't conflict
                val yearAwareKey = if (!entry.year.isNullOrBlank()) "$normalizedTitle|${entry.year}" else null
                val titleFallback = (existingItems[normalizedTitle] ?: existingItems[entry.title.trim().lowercase()])?.takeIf { candidate ->
                    val candidateYear = candidate.releaseDate?.take(4)
                    candidateYear.isNullOrBlank() || entry.year.isNullOrBlank() || candidateYear == entry.year
                }
                val existing = (if (yearAwareKey != null) existingItems[yearAwareKey] else null) ?: titleFallback

                if (existing != null) {
                    val hasNewerWatch = entry.watchedAt > (existing.watchedAt ?: 0L)
                    val hasNewRating = entry.ratingTenScale != null && existing.rating == null
                    val hasNewNotes = !entry.notes.isNullOrBlank() && existing.userNotes.isNullOrBlank()
                    val hasNewTmdb = !entry.tmdbId.isNullOrBlank() && existing.tmdbId.isNullOrBlank()
                    val hasNewPoster = !entry.posterUrl.isNullOrBlank() && existing.imageUrl.isNullOrBlank()
                    val needsStatusUpgrade = existing.status != MediaStatus.WATCHED.name

                    if (needsStatusUpgrade || hasNewerWatch || hasNewRating || hasNewNotes || hasNewTmdb || hasNewPoster) {
                        // Preserve original import source (e.g. podcast) alongside the RSS sync note
                        val combinedSource = if (!existing.importSource.isNullOrBlank() &&
                            !existing.importSource.contains("Letterboxd Live RSS")
                        ) {
                            "${existing.importSource}, Letterboxd Live RSS (@$cleanUser)"
                        } else {
                            "Letterboxd Live RSS (@$cleanUser)"
                        }
                        val updated = existing.copy(
                            status = MediaStatus.WATCHED.name,
                            watchedAt = if (hasNewerWatch || existing.watchedAt == null) entry.watchedAt else existing.watchedAt,
                            rating = entry.ratingTenScale ?: existing.rating,
                            userNotes = (entry.notes ?: existing.userNotes)?.take(300),
                            importSource = combinedSource.take(200),
                            tmdbId = existing.tmdbId ?: entry.tmdbId,
                            imageUrl = existing.imageUrl ?: entry.posterUrl,
                            releaseDate = existing.releaseDate ?: entry.year?.takeIf { it.isNotBlank() },
                            overview = (existing.overview ?: "Imported from Letterboxd diary: ${entry.title} (${entry.year ?: "N/A"}). Logged on ${entry.dateString}.").take(300),
                            updatedAt = System.currentTimeMillis()
                        )
                        mediaDao.updateMediaItem(updated)
                        if (needsStatusUpgrade || hasNewerWatch) {
                            mediaDao.insertWatchSession(
                                WatchSession(
                                    mediaItemId = updated.id,
                                    mediaItemTitle = updated.title,
                                    providerId = updated.providersList.firstOrNull(),
                                    watchedAt = entry.watchedAt,
                                    durationMinutes = 110,
                                    notes = "Synced via Letterboxd Live RSS (@$cleanUser)"
                                )
                            )
                        }
                        existingItems[normalizedTitle] = updated
                        if (yearAwareKey != null) existingItems[yearAwareKey] = updated
                        newlyImported.add(updated)
                        Log.d(TAG, "Promoted '${entry.title}' from ${existing.status} → WATCHED (watchedAt=${entry.dateString})")
                    }
                } else {
                    val item = MediaItem(
                        title = entry.title,
                        sharedUrl = entry.link,
                        status = MediaStatus.WATCHED.name,
                        addedAt = entry.watchedAt,
                        watchedAt = entry.watchedAt,
                        tmdbId = entry.tmdbId,
                        imageUrl = entry.posterUrl,
                        rating = entry.ratingTenScale,
                        userNotes = entry.notes?.take(300),
                        importSource = "Letterboxd Live RSS (@$cleanUser)",
                        releaseDate = entry.year?.takeIf { it.isNotBlank() },
                        overview = "Imported from Letterboxd diary: ${entry.title} (${entry.year ?: "N/A"}). Logged on ${entry.dateString}.".take(300)
                    )
                    val insertedId = mediaDao.insertMediaItem(item)
                    val insertedItem = item.copy(id = insertedId)
                    mediaDao.insertWatchSession(
                        WatchSession(
                            mediaItemId = insertedId,
                            mediaItemTitle = insertedItem.title,
                            providerId = null,
                            watchedAt = entry.watchedAt,
                            durationMinutes = 110,
                            notes = "Synced via Letterboxd Live RSS (@$cleanUser)"
                        )
                    )
                    existingItems[normalizedTitle] = insertedItem
                    if (yearAwareKey != null) existingItems[yearAwareKey] = insertedItem
                    newlyImported.add(insertedItem)
                    Log.d(TAG, "Inserted new WATCHED '${entry.title}' (${entry.year}) watchedAt=${entry.dateString}")
                }
            }

            mediaDao.deduplicateMediaItems()

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

    internal data class ParsedRssEntry(
        val title: String,
        val year: String?,
        val link: String?,
        val dateString: String,
        val watchedAt: Long,
        val ratingTenScale: Double?,
        val notes: String?,
        val tmdbId: String? = null,
        val posterUrl: String? = null,
        val isRewatch: Boolean = false
    )

    internal fun parseRssXml(xml: String): List<ParsedRssEntry> {
        val entries = mutableListOf<ParsedRssEntry>()
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }
        val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val rfcDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)

        val items = doc.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val itemNode = items.item(i)
            if (itemNode.nodeType != Node.ELEMENT_NODE) continue
            val itemElem = itemNode as Element

            var curTitle: String? = null
            var curFilmTitle: String? = null
            var curFilmYear: String? = null
            var curLink: String? = null
            var curWatchedDate: String? = null
            var curRating: String? = null
            var curPubDate: String? = null
            var curDescription: String? = null
            var curTmdbId: String? = null
            var curRewatch: String? = null

            val children = itemElem.childNodes
            for (j in 0 until children.length) {
                val child = children.item(j)
                if (child.nodeType != Node.ELEMENT_NODE) continue
                val cleanTag = child.nodeName.substringAfterLast(':').lowercase()
                val text = child.textContent?.trim()
                when (cleanTag) {
                    "title" -> curTitle = text
                    "link" -> curLink = text
                    "filmtitle" -> curFilmTitle = text
                    "filmyear" -> curFilmYear = text
                    "watcheddate" -> curWatchedDate = text
                    "memberrating" -> curRating = text
                    "pubdate" -> curPubDate = text
                    "description" -> curDescription = text
                    "movieid" -> curTmdbId = text
                    "rewatch" -> curRewatch = text
                }
            }

            val rawTitle = curFilmTitle ?: extractTitleFromRssTitle(curTitle)
            val finalTitle = unescapeHtmlEntities(rawTitle)
            if (!finalTitle.isNullOrBlank()) {
                val watchDateTimestamp = try {
                    if (!curWatchedDate.isNullOrBlank()) {
                        dateFormat.parse(curWatchedDate)?.time ?: System.currentTimeMillis()
                    } else if (!curPubDate.isNullOrBlank()) {
                        rfcDateFormat.parse(curPubDate)?.time ?: System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    }
                } catch (_: Exception) {
                    System.currentTimeMillis()
                }

                val ratingTen = curRating?.toDoubleOrNull()?.let { it * 2.0 } // 5-star to 10-scale
                val posterUrl = extractPosterUrl(curDescription)
                val cleanNotes = cleanHtmlDescription(curDescription)

                entries.add(
                    ParsedRssEntry(
                        title = finalTitle,
                        year = curFilmYear?.takeIf { it.isNotBlank() },
                        link = curLink,
                        dateString = curWatchedDate ?: curPubDate ?: "Recent",
                        watchedAt = watchDateTimestamp,
                        ratingTenScale = ratingTen,
                        notes = cleanNotes,
                        tmdbId = curTmdbId?.takeIf { it.isNotBlank() && it != "0" },
                        posterUrl = posterUrl,
                        isRewatch = curRewatch.equals("Yes", ignoreCase = true)
                    )
                )
            }
        }
        return entries
    }

    private fun extractPosterUrl(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val match = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.startsWith("http") && it.length <= 500 }
    }

    private fun unescapeHtmlEntities(text: String?): String? {
        if (text == null) return null
        return text
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
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
        val unescaped = unescapeHtmlEntities(stripped) ?: ""
        // Strip Letterboxd's default non-review boilerplate: "Watched on Saturday November 14, 2026."
        val withoutBoilerplate = unescaped
            .replace(Regex("""^Watched on [A-Za-z]+,?\s+[A-Za-z]+\s+\d{1,2},\s+\d{4}\.?$""", RegexOption.IGNORE_CASE), "")
            .trim()
        return withoutBoilerplate.takeIf { it.isNotBlank() }
    }
}
