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
    version = 9,
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE streaming_providers ADD COLUMN subscribedSince INTEGER")
                db.execSQL("ALTER TABLE streaming_providers ADD COLUMN planName TEXT")
                db.execSQL("ALTER TABLE streaming_providers ADD COLUMN renewalDayOfMonth INTEGER")
                db.execSQL("ALTER TABLE streaming_providers ADD COLUMN notes TEXT")

                val now = System.currentTimeMillis()
                val expanded = listOf(
                    Triple("paramount", "Paramount+", 5.99),
                    Triple("peacock", "Peacock", 5.99),
                    Triple("criterion", "Criterion Channel", 10.99),
                    Triple("mubi", "MUBI", 14.99),
                    Triple("shudder", "Shudder", 6.99),
                    Triple("starz", "Starz", 9.99),
                    Triple("britbox", "BritBox", 8.99),
                    Triple("amc", "AMC+", 8.99),
                    Triple("kanopy", "Kanopy", 0.0),
                    Triple("hoopla", "Hoopla", 0.0)
                )
                for ((id, name, cost) in expanded) {
                    db.execSQL("INSERT OR IGNORE INTO streaming_providers (id, name, costPerMonth, isActive, updatedAt) VALUES ('$id', '$name', $cost, 0, $now)")
                }
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN releaseDate TEXT")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Purge non-movie podcast discussions from Watchlist and trim oversized text columns
                db.execSQL("DELETE FROM media_items WHERE status = 'WATCHLIST' AND (tmdbId IS NULL OR tmdbId = '')")
                db.execSQL("UPDATE media_items SET userNotes = substr(userNotes, 1, 300) WHERE length(userNotes) > 300")
                db.execSQL("UPDATE media_items SET overview = substr(overview, 1, 300) WHERE length(overview) > 300")
                db.execSQL("UPDATE media_items SET trivia = NULL WHERE trivia IS NOT NULL")
            }
        }
    }

    private class DatabaseCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        private val isSeeding = java.util.concurrent.atomic.AtomicBoolean(false)

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

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            try {
                // Synchronously purge non-movie podcast entries from Watchlist before DAO cursor iteration
                db.execSQL("DELETE FROM media_items WHERE status = 'WATCHLIST' AND (tmdbId IS NULL OR tmdbId = '')")
                db.execSQL("UPDATE media_items SET userNotes = substr(userNotes, 1, 300) WHERE length(userNotes) > 300")
                db.execSQL("UPDATE media_items SET overview = substr(overview, 1, 300) WHERE length(overview) > 300")
                db.execSQL("UPDATE media_items SET trivia = NULL WHERE trivia IS NOT NULL")
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error during onOpen integrity scrub", e)
            }
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val watchedCount = database.mediaDao().getWatchedCount()
                    if (watchedCount < 1000) {
                        Log.d("AppDatabase", "Detected only $watchedCount watched items, supplementing from full Letterboxd archive...")
                        populateInitialProvidersAndLetterboxdData(database.mediaDao())
                    }
                    // Ensure all 19 streaming providers exist in database
                    val existing = database.mediaDao().getAllStreamingProviders().map { it.id }.toSet()
                    val missing = DEFAULT_STREAMING_PROVIDERS.filter { !existing.contains(it.id) }
                    if (missing.isNotEmpty()) {
                        database.mediaDao().insertStreamingProviders(missing)
                        Log.d("AppDatabase", "Seeded ${missing.size} missing streaming providers on database open.")
                    }
                    val removed = database.mediaDao().deduplicateMediaItems()
                    if (removed > 0) {
                        Log.d("AppDatabase", "Cleaned up $removed duplicate media items on database open.")
                    }
                }
            }
        }

        private val DEFAULT_STREAMING_PROVIDERS = listOf(
            StreamingProvider("netflix", "Netflix", costPerMonth = 15.49, isActive = true),
            StreamingProvider("hulu", "Hulu", costPerMonth = 14.99, isActive = true),
            StreamingProvider("max", "Max (HBO)", costPerMonth = 15.99, isActive = true),
            StreamingProvider("disney", "Disney+", costPerMonth = 13.99, isActive = true),
            StreamingProvider("prime", "Prime Video", costPerMonth = 8.99, isActive = false),
            StreamingProvider("apple", "Apple TV+", costPerMonth = 9.99, isActive = false),
            StreamingProvider("paramount", "Paramount+", costPerMonth = 5.99, isActive = false),
            StreamingProvider("peacock", "Peacock", costPerMonth = 5.99, isActive = false),
            StreamingProvider("criterion", "Criterion Channel", costPerMonth = 10.99, isActive = false),
            StreamingProvider("mubi", "MUBI", costPerMonth = 14.99, isActive = false),
            StreamingProvider("shudder", "Shudder", costPerMonth = 6.99, isActive = false),
            StreamingProvider("starz", "Starz", costPerMonth = 9.99, isActive = false),
            StreamingProvider("britbox", "BritBox", costPerMonth = 8.99, isActive = false),
            StreamingProvider("amc", "AMC+", costPerMonth = 8.99, isActive = false),
            StreamingProvider("tubi", "Tubi", costPerMonth = 0.0, isActive = true),
            StreamingProvider("freevee", "Freevee", costPerMonth = 0.0, isActive = true),
            StreamingProvider("pluto", "Pluto TV", costPerMonth = 0.0, isActive = true),
            StreamingProvider("kanopy", "Kanopy", costPerMonth = 0.0, isActive = true),
            StreamingProvider("hoopla", "Hoopla", costPerMonth = 0.0, isActive = true)
        )

        private suspend fun populateInitialProvidersAndLetterboxdData(dao: MediaDao) {
            if (!isSeeding.compareAndSet(false, true)) {
                Log.d("AppDatabase", "Database seeding already in progress. Skipping redundant concurrent execution.")
                return
            }
            try {
                dao.insertStreamingProviders(DEFAULT_STREAMING_PROVIDERS)

                val existingTitles = dao.getAllTitles().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toMutableSet()

                Log.d("AppDatabase", "Seeding initial Letterboxd watch list entries from CSV...")
                val watchlistRows = parseCsv(context, "watchlist.csv")
                val watchlistItems = watchlistRows
                    .filter { row ->
                        val key = row.name.trim().lowercase()
                        if (key.isNotEmpty() && !existingTitles.contains(key)) {
                            existingTitles.add(key)
                            true
                        } else false
                    }
                    .map { row ->
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
                    Log.d("AppDatabase", "Seeded ${watchlistItems.size} new watchlist items from CSV successfully.")
                }

                Log.d("AppDatabase", "Seeding Letterboxd watched history entries and monthly watch sessions from CSV...")
                val historyRows = parseCsv(context, "watched_history.csv")
                val newHistoryRows = historyRows.filter { row ->
                    val key = row.name.trim().lowercase()
                    if (key.isNotEmpty() && !existingTitles.contains(key)) {
                        existingTitles.add(key)
                        true
                    } else false
                }
                val historyItems = newHistoryRows.map { row ->
                    val watchTimestamp = parseDateToTimestamp(row.date)
                    MediaItem(
                        title = row.name,
                        sharedUrl = row.uri,
                        status = com.example.data.model.MediaStatus.WATCHED.name,
                        addedAt = watchTimestamp,
                        watchedAt = watchTimestamp,
                        providerIds = null,
                        userNotes = row.notes,
                        importSource = row.source,
                        overview = "Imported movie logged as watched on ${row.date} from Letterboxd archive."
                    )
                }
                if (historyItems.isNotEmpty()) {
                    val historyIds = dao.insertMediaItems(historyItems)
                    Log.d("AppDatabase", "Inserted ${historyItems.size} watched movies from CSV successfully.")

                    val watchSessions = newHistoryRows.mapIndexed { index, row ->
                        val mediaItemId = historyIds.getOrElse(index) { 0L }
                        val providerId = DEFAULT_STREAMING_PROVIDERS[index % DEFAULT_STREAMING_PROVIDERS.size].id
                        WatchSession(
                            mediaItemId = mediaItemId,
                            mediaItemTitle = row.name,
                            providerId = providerId,
                            watchedAt = parseDateToTimestamp(row.date),
                            durationMinutes = 120,
                            notes = "Seeded watch session log from Letterboxd movie archive. ${row.notes ?: ""}"
                        )
                    }
                    dao.insertWatchSessions(watchSessions)
                    Log.d("AppDatabase", "Seeded ${watchSessions.size} historical watch sessions from CSV successfully.")
                }
                dao.deduplicateMediaItems()
            } finally {
                isSeeding.set(false)
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
