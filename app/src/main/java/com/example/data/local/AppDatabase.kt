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
    version = 10,
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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
                db.execSQL("ALTER TABLE streaming_providers ADD COLUMN userCostPerMonth REAL")
                db.execSQL("ALTER TABLE streaming_providers ADD COLUMN subscriptionStartDate INTEGER")
                db.execSQL("ALTER TABLE streaming_providers ADD COLUMN trialEndDate INTEGER")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN userRating REAL")
                db.execSQL("ALTER TABLE media_items ADD COLUMN isRewatch INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN letterboxdUri TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN syncedToSheet INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_items ADD COLUMN runtimeMinutes INTEGER")
                db.execSQL("ALTER TABLE media_items ADD COLUMN releaseYear TEXT")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'MOVIE'")
                db.execSQL("ALTER TABLE media_items ADD COLUMN totalSeasons INTEGER")
                db.execSQL("ALTER TABLE media_items ADD COLUMN totalEpisodes INTEGER")
                db.execSQL("ALTER TABLE media_items ADD COLUMN lastWatchedSeason INTEGER")
                db.execSQL("ALTER TABLE media_items ADD COLUMN lastWatchedEpisode INTEGER")
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_items ADD COLUMN nextAirDate TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN nextEpisodeTitle TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN releaseStatus TEXT")
                db.execSQL("ALTER TABLE media_items ADD COLUMN digitalReleaseDate TEXT")
                // Seed initial release radar status for Severance and The Bear
                db.execSQL("UPDATE media_items SET releaseStatus = 'RETURNING_SERIES', nextAirDate = '2026-10-17', nextEpisodeTitle = 'S2E1: Hello Ms. Cobel' WHERE title LIKE '%Severance%'")
                db.execSQL("UPDATE media_items SET releaseStatus = 'RETURNING_SERIES', nextAirDate = '2026-06-25', nextEpisodeTitle = 'S4E1: The Kitchen' WHERE title LIKE '%The Bear%'")
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
                    letterboxdUri = row.uri,
                    releaseYear = row.year,
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
                    letterboxdUri = row.uri,
                    releaseYear = row.year,
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

            // Seed Curated TV Shows and Film Podcast Recommendations
            val curatedItems = listOf(
                MediaItem(
                    title = "Severance",
                    releaseYear = "2022",
                    mediaType = "TV",
                    totalSeasons = 2,
                    totalEpisodes = 19,
                    status = com.example.data.model.MediaStatus.WATCHLIST.name,
                    providerIds = "apple",
                    overview = "Mark leads a team of office workers whose memories have been surgically divided between their work and personal lives.",
                    releaseStatus = "RETURNING_SERIES",
                    nextAirDate = "2026-10-17",
                    nextEpisodeTitle = "S2E1: Hello Ms. Cobel"
                ),
                MediaItem(
                    title = "The Bear",
                    releaseYear = "2022",
                    mediaType = "TV",
                    totalSeasons = 4,
                    totalEpisodes = 38,
                    status = com.example.data.model.MediaStatus.WATCHLIST.name,
                    providerIds = "hulu",
                    overview = "A young chef from the fine dining world returns to Chicago to run his family's Italian beef sandwich shop.",
                    releaseStatus = "RETURNING_SERIES",
                    nextAirDate = "2026-06-25",
                    nextEpisodeTitle = "S4E1: The Kitchen"
                ),
                MediaItem(
                    title = "Shōgun",
                    releaseYear = "2024",
                    mediaType = "TV",
                    totalSeasons = 1,
                    totalEpisodes = 10,
                    status = com.example.data.model.MediaStatus.WATCHLIST.name,
                    providerIds = "hulu",
                    overview = "When a mysterious European ship is found marooned in a nearby fishing village, Lord Toranaga discovers secrets that could tip the scales of power."
                ),
                MediaItem(
                    title = "Cure",
                    releaseYear = "1997",
                    mediaType = "MOVIE",
                    status = com.example.data.model.MediaStatus.WATCHLIST.name,
                    providerIds = "criterion,tubi",
                    importSource = "Podcast: The Big Picture",
                    userNotes = "Sean Fennessey: 'One of the greatest psychological thrillers of the 90s, masterclass in dread.'",
                    runtimeMinutes = 111,
                    overview = "A detective investigates a series of gruesome murders where each victim has an X carved into their neck, committed by different people who claim no memory of why they did it."
                ),
                MediaItem(
                    title = "Heat",
                    releaseYear = "1995",
                    mediaType = "MOVIE",
                    status = com.example.data.model.MediaStatus.WATCHLIST.name,
                    providerIds = "netflix",
                    importSource = "Podcast: The Rewatchables",
                    userNotes = "Bill Simmons & Chris Ryan: 'The definitive crime epic and apex mountain for Pacino and De Niro together.'",
                    runtimeMinutes = 170,
                    overview = "A master criminal and a veteran cop play a high-stakes cat-and-mouse game through the streets of Los Angeles."
                ),
                MediaItem(
                    title = "Blow Out",
                    releaseYear = "1981",
                    mediaType = "MOVIE",
                    status = com.example.data.model.MediaStatus.WATCHLIST.name,
                    providerIds = "criterion,max",
                    importSource = "Podcast: Blank Check",
                    userNotes = "David Sims & Griffin Newman: 'Brian De Palma firing on every cylinder with John Travolta\\'s most haunting performance.'",
                    runtimeMinutes = 108,
                    overview = "A movie sound recordist accidentally records the audio evidence of a political assassination, putting him in deadly danger."
                )
            )
            dao.insertMediaItems(curatedItems)
            Log.d("AppDatabase", "Seeded ${curatedItems.size} curated TV shows and podcast recommendations.")
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
