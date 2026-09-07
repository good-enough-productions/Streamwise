package com.example.data.local

import androidx.room.*
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.StreamingProvider
import com.example.data.model.WatchSession
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    // --- Media Items (Watchlist) Queries ---

    @Query("SELECT * FROM media_items ORDER BY addedAt DESC")
    fun getAllMediaItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaItemById(id: Long): MediaItem?

    @Query("SELECT * FROM media_items WHERE status = :status ORDER BY addedAt DESC")
    fun getMediaItemsByStatus(status: String): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE status = 'INTENDING_TO_WATCH'")
    suspend fun getIntendingToWatchItems(): List<MediaItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaItem>): List<Long>

    @Update
    suspend fun updateMediaItem(item: MediaItem)

    @Update
    suspend fun updateMediaItems(items: List<MediaItem>)

    @Delete
    suspend fun deleteMediaItem(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaItemById(id: Long)

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getMediaItemCount(): Int

    @Query("SELECT COUNT(*) FROM media_items WHERE status = 'WATCHED'")
    suspend fun getWatchedCount(): Int

    @Query("SELECT COUNT(*) FROM media_items WHERE status = 'WATCHLIST'")
    suspend fun getWatchlistCount(): Int

    @Query("SELECT title FROM media_items")
    suspend fun getAllTitles(): List<String>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaItemsList(): List<MediaItem>

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteMediaItemsByIdList(ids: List<Long>): Int

    @Query("DELETE FROM watch_sessions WHERE mediaItemId NOT IN (SELECT id FROM media_items)")
    suspend fun deleteOrphanedWatchSessions(): Int

    @Transaction
    suspend fun deduplicateMediaItems(): Int {
        val all = getAllMediaItemsList()
        val duplicates = all.groupBy { it.title.trim().lowercase() }
        val idsToDelete = mutableListOf<Long>()
        for ((_, group) in duplicates) {
            if (group.size > 1) {
                val sorted = group.sortedWith(
                    compareByDescending<MediaItem> { it.status == MediaStatus.WATCHED.name }
                        .thenByDescending { !it.imageUrl.isNullOrBlank() }
                        .thenByDescending { !it.tmdbId.isNullOrBlank() }
                        .thenByDescending { !it.providerIds.isNullOrBlank() }
                        .thenBy { it.id }
                )
                idsToDelete.addAll(sorted.drop(1).map { it.id })
            }
        }
        if (idsToDelete.isNotEmpty()) {
            idsToDelete.chunked(500).forEach { chunk ->
                deleteMediaItemsByIdList(chunk)
            }
        }
        deleteOrphanedWatchSessions()
        return idsToDelete.size
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchSessions(sessions: List<WatchSession>)


    // --- Streaming Providers Queries ---

    @Query("SELECT * FROM streaming_providers ORDER BY name ASC")
    fun getAllStreamingProvidersFl(): Flow<List<StreamingProvider>>

    @Query("SELECT * FROM streaming_providers ORDER BY name ASC")
    suspend fun getAllStreamingProviders(): List<StreamingProvider>

    @Query("SELECT * FROM streaming_providers WHERE isActive = 1")
    fun getActiveStreamingProvidersFl(): Flow<List<StreamingProvider>>

    @Query("SELECT * FROM streaming_providers WHERE isActive = 1")
    suspend fun getActiveStreamingProviders(): List<StreamingProvider>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamingProviders(providers: List<StreamingProvider>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreamingProvider(provider: StreamingProvider)


    // --- Watch Sessions Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchSession(session: WatchSession): Long

    @Query("SELECT * FROM watch_sessions ORDER BY watchedAt DESC")
    fun getAllWatchSessions(): Flow<List<WatchSession>>

    @Query("SELECT * FROM watch_sessions ORDER BY watchedAt DESC")
    suspend fun getAllWatchSessionsList(): List<WatchSession>


    // --- ROI & Monthly Analytics Queries ---

    /**
     * Calculates the total minutes watched per active streaming provider for a specific month.
     * Generates cost-efficiency details (cost per hour) to guide users on which subscriptions to pause.
     */
    @Query("""
        SELECT 
            sp.id AS providerId, 
            sp.name AS providerName, 
            sp.costPerMonth AS costPerMonth, 
            sp.isActive AS isActive,
            COALESCE(SUM(ws.durationMinutes), 0) AS totalMinutes
        FROM streaming_providers sp
        LEFT JOIN watch_sessions ws ON sp.id = ws.providerId 
            AND ws.watchedAt >= :startOfMonthTimestamp 
            AND ws.watchedAt <= :endOfMonthTimestamp
        WHERE sp.isActive = 1
        GROUP BY sp.id
    """)
    fun getMonthlyUsageStats(startOfMonthTimestamp: Long, endOfMonthTimestamp: Long): Flow<List<ProviderUsageStats>>
}

/**
 * Data class representing aggregated statistics for subscription optimization.
 * This satisfies the "calculate total hours watched per provider per month" requirement.
 */
data class ProviderUsageStats(
    val providerId: String,
    val providerName: String,
    val costPerMonth: Double,
    val isActive: Boolean,
    val totalMinutes: Long
) {
    val totalHours: Double
        get() = totalMinutes / 60.0

    // Financial ROI: Higher ratio = better. Lower hours watched = high cost per hour = prime cancel candidate!
    val costPerHour: Double
        get() = if (totalHours > 0.0) costPerMonth / totalHours else costPerMonth
}
