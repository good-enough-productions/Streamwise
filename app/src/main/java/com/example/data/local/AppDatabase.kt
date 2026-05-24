package com.example.data.local

import android.content.Context
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
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialProviders(database.mediaDao())
                }
            }
        }

        private suspend fun populateInitialProviders(dao: MediaDao) {
            val providers = listOf(
                StreamingProvider("netflix", "Netflix", costPerMonth = 15.49, isActive = true),
                StreamingProvider("hulu", "Hulu", costPerMonth = 14.99, isActive = true),
                StreamingProvider("max", "Max (HBO)", costPerMonth = 15.99, isActive = true),
                StreamingProvider("disney", "Disney+", costPerMonth = 13.99, isActive = true),
                StreamingProvider("prime", "Prime Video", costPerMonth = 8.99, isActive = false),
                StreamingProvider("apple", "Apple TV+", costPerMonth = 9.99, isActive = false)
            )
            dao.insertStreamingProviders(providers)
            
            // Seed a few demo watchlist items too so the app feels alive on first run!
            val movie1 = MediaItem(
                title = "The White Lotus",
                status = "WATCHLIST",
                providerIds = "max",
                overview = "A sharp social satire following the exploits of various employees and guests at an exclusive Hawaiian resort."
            )
            val movie2 = MediaItem(
                title = "Stranger Things",
                status = "WATCHLIST",
                providerIds = "netflix",
                overview = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments and terrifying supernatural forces."
            )
            val movie3 = MediaItem(
                title = "Shōgun",
                status = "WATCHLIST",
                providerIds = "hulu",
                overview = "In Japan in the year 1600, Lord Yoshii Toranaga is fighting for his life as his enemies on the Council of Regents unite against him."
            )
            dao.insertMediaItem(movie1)
            dao.insertMediaItem(movie2)
            dao.insertMediaItem(movie3)
        }
    }
}
