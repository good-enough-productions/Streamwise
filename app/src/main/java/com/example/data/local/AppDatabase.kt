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
    version = 6,
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context.applicationContext, scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN trivia TEXT")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN userNotes TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN importSource TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN genres TEXT")
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT OR IGNORE INTO streaming_providers (id, name, costPerMonth, isActive, updatedAt) VALUES ('tubi', 'Tubi', 0.0, 1, ${System.currentTimeMillis()})")
                db.execSQL("INSERT OR IGNORE INTO streaming_providers (id, name, costPerMonth, isActive, updatedAt) VALUES ('freevee', 'Freevee', 0.0, 1, ${System.currentTimeMillis()})")
                db.execSQL("INSERT OR IGNORE INTO streaming_providers (id, name, costPerMonth, isActive, updatedAt) VALUES ('pluto', 'Pluto TV', 0.0, 1, ${System.currentTimeMillis()})")
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN watchedAt INTEGER")
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

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
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
                StreamingProvider("apple", "Apple TV+", costPerMonth = 9.99, isActive = false),
                StreamingProvider("tubi", "Tubi", costPerMonth = 0.0, isActive = true),
                StreamingProvider("freevee", "Freevee", costPerMonth = 0.0, isActive = true),
                StreamingProvider("pluto", "Pluto TV", costPerMonth = 0.0, isActive = true)
            )
            dao.insertStreamingProviders(providers)

            Log.d("AppDatabase", "Seeding initial Letterboxd watch list entries from CSV...")
            val watchlistRows = parseCsv(context, "watchlist.csv")
            val watchlistItems = watchlistRows.map { row ->
                MediaItem(
                    title = row.name,
                    sharedUrl = row.uri,
                    status = com.example.data.model.MediaStatus.WATCHLIST.name,
                    addedAt = parseDateToTimestamp(row.date),
                    providerIds = null,
                    userNotes = row.notes,
                    importSource = row.source,
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
                val watchTimestamp = parseDateToTimestamp(row.date)
                MediaItem(
                    title = row.name,
                    sharedUrl = row.uri,
                    status = com.example.data.model.MediaStatus.WATCHED.name,
                    addedAt = watchTimestamp,
                    watchedAt = watchTimestamp, // Populate new watchedAt column
                    providerIds = null,
                    userNotes = row.notes,
                    importSource = row.source,
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
                        notes = "Seeded watch session log from Letterboxd movie theater check-in archive. ${row.notes ?: ""}"
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
                        var inQuotes = false
                        val currentField = StringBuilder()
                        val currentRow = mutableListOf<String>()
                        var isHeader = true
                        
                        var charInt: Int
                        while (reader.read().also { charInt = it } != -1) {
                            val c = charInt.toChar()
                            if (inQuotes) {
                                if (c == '"') {
                                    reader.mark(1)
                                    val nextChar = reader.read()
                                    if (nextChar == '"'.code) {
                                        currentField.append('"')
                                    } else {
                                        inQuotes = false
                                        reader.reset()
                                    }
                                } else {
                                    currentField.append(c)
                                }
                            } else {
                                if (c == '"') {
                                    inQuotes = true
                                } else if (c == ',') {
                                    currentRow.add(currentField.toString().trim())
                                    currentField.setLength(0)
                                } else if (c == '\r') {
                                    // Ignore CR
                                } else if (c == '\n') {
                                    currentRow.add(currentField.toString().trim())
                                    currentField.setLength(0)
                                    
                                    if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                        if (isHeader && currentRow.any { it.contains("Name", ignoreCase = true) }) {
                                            isHeader = false
                                        } else if (!isHeader || !currentRow.any { it.contains("Name", ignoreCase = true) }) {
                                            isHeader = false
                                            if (currentRow.size >= 3) {
                                                list.add(CsvRow(
                                                    date = currentRow.getOrNull(0) ?: "",
                                                    name = currentRow.getOrNull(1) ?: "",
                                                    year = currentRow.getOrNull(2) ?: "",
                                                    uri = currentRow.getOrNull(3) ?: "",
                                                    notes = currentRow.getOrNull(4),
                                                    source = currentRow.getOrNull(5)
                                                ))
                                            }
                                        }
                                    }
                                    currentRow.clear()
                                } else {
                                    currentField.append(c)
                                }
                            }
                        }
                        // Handle the last line if it doesn't end with a newline
                        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
                            currentRow.add(currentField.toString().trim())
                            if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                if (!isHeader || !currentRow.any { it.contains("Name", ignoreCase = true) }) {
                                    if (currentRow.size >= 3) {
                                        list.add(CsvRow(
                                            date = currentRow.getOrNull(0) ?: "",
                                            name = currentRow.getOrNull(1) ?: "",
                                            year = currentRow.getOrNull(2) ?: "",
                                            uri = currentRow.getOrNull(3) ?: "",
                                            notes = currentRow.getOrNull(4),
                                            source = currentRow.getOrNull(5)
                                        ))
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error parsing CSV file: $fileName", e)
            }
            return list
        }

        private fun parseDateToTimestamp(dateStr: String): Long {
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                sdf.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }

        private data class CsvRow(
            val date: String, 
            val name: String, 
            val year: String, 
            val uri: String,
            val notes: String? = null,
            val source: String? = null
        )
    }
}
