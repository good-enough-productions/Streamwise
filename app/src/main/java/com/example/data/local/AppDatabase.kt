package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.MediaItem
import com.example.data.model.StreamingProvider
import com.example.data.model.WatchSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@Database(
    entities = [MediaItem::class, StreamingProvider::class, WatchSession::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stream_manager_database"
                )
                .addCallback(DatabaseCallback(context.applicationContext, scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialProvidersAndLetterboxdData(database.mediaDao())
                }
            }
        }

        private suspend fun populateInitialProvidersAndLetterboxdData(dao: MediaDao) {
            val providers = listOf(
                StreamingProvider("netflix", "Netflix", costPerMonth = 15.49, isActive = true),
                StreamingProvider("hulu", "Hulu", costPerMonth = 14.99, isActive = true),
                StreamingProvider("max", "Max (HBO)", costPerMonth = 15.99, isActive = true),
                StreamingProvider("disney", "Disney+", costPerMonth = 13.99, isActive = true),
                StreamingProvider("prime", "Prime Video", costPerMonth = 8.99, isActive = false),
                StreamingProvider("apple", "Apple TV+", costPerMonth = 9.99, isActive = false)
            )
            dao.insertStreamingProviders(providers)

            Log.d("AppDatabase", "Seeding initial Letterboxd watch list entries from CSV...")
            val watchlistRows = parseCsv(context, "watchlist.csv")
            val watchlistItems = watchlistRows.map { row ->
                val randomProviders = providers.shuffled().take(2).joinToString(",") { it.id }
                MediaItem(
                    title = row.name,
                    sharedUrl = row.uri,
                    status = com.example.data.model.MediaStatus.WATCHLIST.name,
                    addedAt = parseDateToTimestamp(row.date),
                    providerIds = randomProviders,
                    overview = "Imported watchlist item \"${row.name}\" from Letterboxd account watchlist record."
                )
            }
            if (watchlistItems.isNotEmpty()) {
                dao.insertMediaItems(watchlistItems)
                Log.d("AppDatabase", "Seeded ${watchlistItems.size} watchlist items from CSV successfully.")
            }

            Log.d("AppDatabase", "Seeding initial Letterboxd watched history entries and monthly watch sessions from CSV...")
            val historyRows = parseCsv(context, "watched_history.csv")
            val historyItems = historyRows.map { row ->
                val randomProviders = providers.shuffled().take(2).joinToString(",") { it.id }
                MediaItem(
                    title = row.name,
                    sharedUrl = row.uri,
                    status = com.example.data.model.MediaStatus.WATCHED.name,
                    addedAt = parseDateToTimestamp(row.date),
                    providerIds = randomProviders,
                    overview = "Imported movie logged as watched on ${row.date} from Letterboxd archive."
                )
            }
            if (historyItems.isNotEmpty()) {
                val historyIds = dao.insertMediaItems(historyItems)
                Log.d("AppDatabase", "Inserted ${historyItems.size} watched movies from CSV successfully.")

                // Now insert corresponding Watch Sessions to populate budget ROI analytics
                val watchSessions = historyRows.mapIndexed { index, row ->
                    val mediaItemId = historyIds.getOrElse(index) { 0L }
                    val providerId = providers[index % providers.size].id
                    WatchSession(
                        mediaItemId = mediaItemId,
                        mediaItemTitle = row.name,
                        providerId = providerId,
                        watchedAt = parseDateToTimestamp(row.date),
                        durationMinutes = 120, // 2 hour movie standard length
                        notes = "Seeded watch session log from Letterboxd movie theater check-in archive."
                    )
                }
                dao.insertWatchSessions(watchSessions)
                Log.d("AppDatabase", "Seeded ${watchSessions.size} historical watch sessions from CSV successfully.")
            }
        }

        private fun parseCsv(context: Context, fileName: String): List<CsvRow> {
            val list = mutableListOf<CsvRow>()
            try {
                context.assets.open(fileName).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        // Check if it's the header, skip if so
                        if (line != null && line.contains("Name", ignoreCase = true)) {
                            // Header skipped
                        } else if (line != null) {
                            val parts = splitCsvLine(line!!)
                            if (parts.size >= 3) {
                                list.add(CsvRow(
                                    date = parts.getOrNull(0) ?: "",
                                    name = parts.getOrNull(1) ?: "",
                                    year = parts.getOrNull(2) ?: "",
                                    uri = parts.getOrNull(3) ?: ""
                                ))
                            }
                        }
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line ?: continue
                            if (currentLine.isBlank()) continue
                            val parts = splitCsvLine(currentLine)
                            if (parts.size >= 3) {
                                list.add(CsvRow(
                                    date = parts.getOrNull(0) ?: "",
                                    name = parts.getOrNull(1) ?: "",
                                    year = parts.getOrNull(2) ?: "",
                                    uri = parts.getOrNull(3) ?: ""
                                ))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error parsing CSV file: $fileName", e)
            }
            return list
        }

        private fun splitCsvLine(line: String): List<String> {
            val result = mutableListOf<String>()
            val currentStr = StringBuilder()
            var inQuotes = false
            for (char in line) {
                if (char == '"') {
                    inQuotes = !inQuotes
                } else if (char == ',' && !inQuotes) {
                    result.add(currentStr.toString().trim())
                    currentStr.setLength(0)
                } else {
                    currentStr.append(char)
                }
            }
            result.add(currentStr.toString().trim())
            return result
        }

        private fun parseDateToTimestamp(dateStr: String): Long {
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                sdf.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        private data class CsvRow(val date: String, val name: String, val year: String, val uri: String)
    }
}
