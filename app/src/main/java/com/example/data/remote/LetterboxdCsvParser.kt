package com.example.data.remote

import android.util.Log
import com.example.data.local.MediaDao
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipInputStream

data class LetterboxdFileImportResult(
    val isSuccess: Boolean,
    val sourceName: String,
    val totalProcessed: Int,
    val watchlistImported: Int,
    val watchedImported: Int,
    val alreadyPresentCount: Int,
    val sampleTitles: List<String>,
    val errorMessage: String? = null
)

class LetterboxdCsvParser(private val mediaDao: MediaDao) {

    companion object {
        private const val TAG = "LetterboxdCsvParser"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }

    /**
     * Parses an input stream which can be either a .zip archive (from letterboxd.com/settings/data)
     * or a direct .csv file (from letterboxd.com/username/watchlist/export/ or similar).
     */
    suspend fun parseAndImportStream(
        inputStream: InputStream,
        filename: String = "letterboxd_data.csv"
    ): LetterboxdFileImportResult = withContext(Dispatchers.IO) {
        try {
            val lowerName = filename.lowercase()
            if (lowerName.endsWith(".zip")) {
                parseZipStream(inputStream, filename)
            } else {
                parseSingleCsvStream(inputStream, filename)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Letterboxd file: ${e.message}", e)
            LetterboxdFileImportResult(
                isSuccess = false,
                sourceName = filename,
                totalProcessed = 0,
                watchlistImported = 0,
                watchedImported = 0,
                alreadyPresentCount = 0,
                sampleTitles = emptyList(),
                errorMessage = e.localizedMessage ?: "Unknown parsing error"
            )
        }
    }

    /**
     * Unpacks a Letterboxd account export ZIP archive and processes relevant CSVs in sequence.
     */
    private suspend fun parseZipStream(
        inputStream: InputStream,
        zipName: String
    ): LetterboxdFileImportResult {
        var totalProcessed = 0
        var watchlistImported = 0
        var watchedImported = 0
        var alreadyPresent = 0
        val sampleTitles = mutableListOf<String>()

        val existingItems = mediaDao.getAllMediaItemsList().associateBy { it.title.trim().lowercase() }.toMutableMap()

        val zipInput = ZipInputStream(inputStream)
        var entry = zipInput.nextEntry

        while (entry != null) {
            val entryName = entry.name.lowercase().substringAfterLast('/')
            if (entryName.endsWith(".csv")) {
                Log.d(TAG, "Examining ZIP entry: $entryName")
                val reader = BufferedReader(InputStreamReader(NonClosingInputStream(zipInput), Charsets.UTF_8))
                val lines = reader.readLines()

                if (lines.isNotEmpty()) {
                    when {
                        entryName == "watchlist.csv" || entryName.contains("watchlist") -> {
                            val res = processCsvRows(lines, isWatchlist = true, existingItems)
                            totalProcessed += res.total
                            watchlistImported += res.imported
                            alreadyPresent += res.duplicates
                            sampleTitles.addAll(res.samples)
                        }
                        entryName == "diary.csv" -> {
                            val res = processDiaryCsvRows(lines, existingItems)
                            totalProcessed += res.total
                            watchedImported += res.imported
                            alreadyPresent += res.duplicates
                            sampleTitles.addAll(res.samples)
                        }
                        entryName == "watched.csv" -> {
                            val res = processCsvRows(lines, isWatchlist = false, existingItems)
                            totalProcessed += res.total
                            watchedImported += res.imported
                            alreadyPresent += res.duplicates
                            sampleTitles.addAll(res.samples)
                        }
                    }
                }
            }
            zipInput.closeEntry()
            entry = zipInput.nextEntry
        }

        return LetterboxdFileImportResult(
            isSuccess = true,
            sourceName = zipName,
            totalProcessed = totalProcessed,
            watchlistImported = watchlistImported,
            watchedImported = watchedImported,
            alreadyPresentCount = alreadyPresent,
            sampleTitles = sampleTitles.distinct().take(10)
        )
    }

    /**
     * Parses a standalone CSV file (e.g. watchlist.csv or diary.csv or watched.csv).
     */
    private suspend fun parseSingleCsvStream(
        inputStream: InputStream,
        csvName: String
    ): LetterboxdFileImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = reader.readLines()
        if (lines.isEmpty()) {
            return LetterboxdFileImportResult(
                isSuccess = false,
                sourceName = csvName,
                totalProcessed = 0,
                watchlistImported = 0,
                watchedImported = 0,
                alreadyPresentCount = 0,
                sampleTitles = emptyList(),
                errorMessage = "CSV file is empty."
            )
        }

        val existingItems = mediaDao.getAllMediaItemsList().associateBy { it.title.trim().lowercase() }.toMutableMap()
        val header = lines.first().lowercase()

        val isDiary = header.contains("rewatch") || header.contains("watched date")
        val isWatchlist = !isDiary && (csvName.lowercase().contains("watchlist") || !header.contains("rating"))

        return if (isDiary) {
            val res = processDiaryCsvRows(lines, existingItems)
            LetterboxdFileImportResult(
                isSuccess = true,
                sourceName = csvName,
                totalProcessed = res.total,
                watchlistImported = 0,
                watchedImported = res.imported,
                alreadyPresentCount = res.duplicates,
                sampleTitles = res.samples.distinct().take(10)
            )
        } else {
            val res = processCsvRows(lines, isWatchlist = isWatchlist, existingItems)
            LetterboxdFileImportResult(
                isSuccess = true,
                sourceName = csvName,
                totalProcessed = res.total,
                watchlistImported = if (isWatchlist) res.imported else 0,
                watchedImported = if (!isWatchlist) res.imported else 0,
                alreadyPresentCount = res.duplicates,
                sampleTitles = res.samples.distinct().take(10)
            )
        }
    }

    private data class RowProcessResult(
        val total: Int,
        val imported: Int,
        val duplicates: Int,
        val samples: List<String>
    )

    /**
     * Standard Letterboxd CSV: Date,Name,Year,Letterboxd URI
     */
    private suspend fun processCsvRows(
        lines: List<String>,
        isWatchlist: Boolean,
        existingMap: MutableMap<String, MediaItem>
    ): RowProcessResult {
        if (lines.size <= 1) return RowProcessResult(0, 0, 0, emptyList())

        val headers = parseCsvLine(lines[0]).map { it.trim().lowercase() }
        val nameIdx = headers.indexOfFirst { it == "name" || it == "title" }
        val dateIdx = headers.indexOfFirst { it == "date" }
        val yearIdx = headers.indexOfFirst { it == "year" }
        val uriIdx = headers.indexOfFirst { it.contains("uri") || it.contains("link") || it.contains("url") }

        if (nameIdx == -1) return RowProcessResult(0, 0, 0, emptyList())

        var imported = 0
        var duplicates = 0
        val samples = mutableListOf<String>()

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val tokens = parseCsvLine(line)
            if (tokens.size <= nameIdx) continue

            val title = tokens[nameIdx].trim()
            if (title.isBlank()) continue

            val normKey = title.lowercase()
            val existing = existingMap[normKey]

            val dateStr = if (dateIdx >= 0 && dateIdx < tokens.size) tokens[dateIdx].trim() else ""
            val yearStr = if (yearIdx >= 0 && yearIdx < tokens.size) tokens[yearIdx].trim() else null
            val uriStr = if (uriIdx >= 0 && uriIdx < tokens.size) tokens[uriIdx].trim() else null

            val parsedTime = try {
                if (dateStr.isNotBlank()) DATE_FORMAT.parse(dateStr)?.time ?: System.currentTimeMillis()
                else System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            if (existing != null) {
                // If existing was watchlist but we are now importing as watched, upgrade it
                val needsStatusUpgrade = !isWatchlist && existing.status != MediaStatus.WATCHED.name
                val hasNewerWatch = !isWatchlist && parsedTime > (existing.watchedAt ?: 0L)
                if (needsStatusUpgrade || hasNewerWatch) {
                    val updated = existing.copy(
                        status = if (!isWatchlist) MediaStatus.WATCHED.name else existing.status,
                        watchedAt = if (!isWatchlist && (hasNewerWatch || existing.watchedAt == null)) parsedTime else existing.watchedAt
                    )
                    mediaDao.updateMediaItem(updated)
                    existingMap[normKey] = updated
                    imported++
                    samples.add(title)
                } else {
                    duplicates++
                }
            } else {
                // Brand new entry
                val item = MediaItem(
                    title = title,
                    sharedUrl = uriStr,
                    status = if (isWatchlist) MediaStatus.WATCHLIST.name else MediaStatus.WATCHED.name,
                    addedAt = parsedTime,
                    watchedAt = if (isWatchlist) null else parsedTime,
                    importSource = if (isWatchlist) "Letterboxd Watchlist CSV" else "Letterboxd Watched CSV",
                    overview = "Imported from Letterboxd: $title (${yearStr ?: "N/A"}). Date: $dateStr."
                )
                val id = mediaDao.insertMediaItem(item)
                existingMap[normKey] = item.copy(id = id)
                imported++
                samples.add(title)
            }
        }

        return RowProcessResult(
            total = lines.size - 1,
            imported = imported,
            duplicates = duplicates,
            samples = samples
        )
    }

    /**
     * Letterboxd Diary CSV: Date,Name,Year,Letterboxd URI,Rating,Rewatch,Tags,Watched Date
     */
    private suspend fun processDiaryCsvRows(
        lines: List<String>,
        existingMap: MutableMap<String, MediaItem>
    ): RowProcessResult {
        if (lines.size <= 1) return RowProcessResult(0, 0, 0, emptyList())

        val headers = parseCsvLine(lines[0]).map { it.trim().lowercase() }
        val nameIdx = headers.indexOfFirst { it == "name" || it == "title" }
        val yearIdx = headers.indexOfFirst { it == "year" }
        val uriIdx = headers.indexOfFirst { it.contains("uri") || it.contains("link") || it.contains("url") }
        val ratingIdx = headers.indexOfFirst { it == "rating" }
        val watchedDateIdx = headers.indexOfFirst { it == "watched date" }
        val logDateIdx = headers.indexOfFirst { it == "date" }

        if (nameIdx == -1) return RowProcessResult(0, 0, 0, emptyList())

        var imported = 0
        var duplicates = 0
        val samples = mutableListOf<String>()

        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue
            val tokens = parseCsvLine(line)
            if (tokens.size <= nameIdx) continue

            val title = tokens[nameIdx].trim()
            if (title.isBlank()) continue

            val normKey = title.lowercase()
            val existing = existingMap[normKey]

            val yearStr = if (yearIdx >= 0 && yearIdx < tokens.size) tokens[yearIdx].trim() else null
            val uriStr = if (uriIdx >= 0 && uriIdx < tokens.size) tokens[uriIdx].trim() else null
            val ratingVal = if (ratingIdx >= 0 && ratingIdx < tokens.size) tokens[ratingIdx].trim().toDoubleOrNull() else null
            // Convert 5-star Letterboxd to 10-point scale
            val ratingTen = ratingVal?.let { it * 2.0 }

            val watchedDateStr = if (watchedDateIdx >= 0 && watchedDateIdx < tokens.size && tokens[watchedDateIdx].isNotBlank()) {
                tokens[watchedDateIdx].trim()
            } else if (logDateIdx >= 0 && logDateIdx < tokens.size) {
                tokens[logDateIdx].trim()
            } else ""

            val watchedTimestamp = try {
                if (watchedDateStr.isNotBlank()) DATE_FORMAT.parse(watchedDateStr)?.time ?: System.currentTimeMillis()
                else System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            if (existing != null) {
                val hasNewerWatch = watchedTimestamp > (existing.watchedAt ?: 0L)
                val hasNewRating = ratingTen != null && existing.rating == null
                val needsStatusUpgrade = existing.status != MediaStatus.WATCHED.name

                if (needsStatusUpgrade || hasNewerWatch || hasNewRating) {
                    val updated = existing.copy(
                        status = MediaStatus.WATCHED.name,
                        watchedAt = if (hasNewerWatch || existing.watchedAt == null) watchedTimestamp else existing.watchedAt,
                        rating = ratingTen ?: existing.rating
                    )
                    mediaDao.updateMediaItem(updated)
                    existingMap[normKey] = updated
                    imported++
                    samples.add(title)
                } else {
                    duplicates++
                }
            } else {
                val item = MediaItem(
                    title = title,
                    sharedUrl = uriStr,
                    status = MediaStatus.WATCHED.name,
                    addedAt = watchedTimestamp,
                    watchedAt = watchedTimestamp,
                    rating = ratingTen,
                    importSource = "Letterboxd Diary CSV",
                    overview = "Imported from Letterboxd Diary: $title (${yearStr ?: "N/A"}). Watched: $watchedDateStr."
                )
                val id = mediaDao.insertMediaItem(item)
                existingMap[normKey] = item.copy(id = id)
                imported++
                samples.add(title)
            }
        }

        return RowProcessResult(
            total = lines.size - 1,
            imported = imported,
            duplicates = duplicates,
            samples = samples
        )
    }

    /**
     * Generates standard Letterboxd import CSV string:
     * Title,Year,Rating10,WatchedDate
     * Compatible with https://letterboxd.com/import/
     */
    fun generateLetterboxdExportCsv(watchedItems: List<MediaItem>): String {
        val sb = StringBuilder()
        sb.append("Title,Year,Rating10,WatchedDate\n")

        val yearRegex = Regex("\\b(19\\d\\d|20\\d\\d)\\b")

        for (item in watchedItems) {
            val cleanTitle = item.title.replace("\"", "\"\"")
            val year = yearRegex.find(item.title)?.value ?: yearRegex.find(item.overview ?: "")?.value ?: ""
            val rating10 = item.rating?.let { String.format(Locale.US, "%.1f", it) } ?: ""
            val watchedDate = item.watchedAt?.let { DATE_FORMAT.format(it) }
                ?: item.addedAt.let { DATE_FORMAT.format(it) }

            sb.append("\"$cleanTitle\",")
            sb.append("$year,")
            sb.append("$rating10,")
            sb.append("$watchedDate\n")
        }

        return sb.toString()
    }

    /**
     * Robust CSV line splitter supporting quoted values and escaped quotes.
     */
    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString())
        return tokens
    }

    /**
     * Helper to prevent ZipInputStream from being closed by individual stream readers.
     */
    private class NonClosingInputStream(private val delegate: InputStream) : InputStream() {
        override fun read(): Int = delegate.read()
        override fun read(b: ByteArray): Int = delegate.read(b)
        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
        override fun close() {
            // Do not close the parent ZipInputStream
        }
    }
}
