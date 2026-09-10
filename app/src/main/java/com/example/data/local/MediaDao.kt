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
        val idsToDelete = mutableSetOf<Long>()
        val itemsToUpdate = mutableListOf<MediaItem>()

        fun mergeGroup(group: List<MediaItem>): Pair<MediaItem, List<Long>> {
            val isWatched = group.any { it.status == MediaStatus.WATCHED.name }
            val bestWatchedAt = group.mapNotNull { it.watchedAt }.maxOrNull()

            val sorted = group.sortedWith(
                compareByDescending<MediaItem> { it.status == MediaStatus.WATCHED.name }
                    .thenByDescending { !it.tmdbId.isNullOrBlank() }
                    .thenByDescending { !it.imageUrl.isNullOrBlank() }
                    .thenByDescending { !it.providerIds.isNullOrBlank() && it.providerIds != "none" }
                    .thenByDescending { (it.rating ?: 0.0) > 0.0 }
                    .thenBy { it.id }
            )
            val primary = sorted.first()
            val losers = sorted.drop(1)

            val richestTmdbId = group.firstOrNull { !it.tmdbId.isNullOrBlank() }?.tmdbId ?: primary.tmdbId
            val richestImage = group.firstOrNull { !it.imageUrl.isNullOrBlank() }?.imageUrl ?: primary.imageUrl
            val richestOverview = group.firstOrNull { !it.overview.isNullOrBlank() && !it.overview.startsWith("Imported") }?.overview
                ?: group.firstOrNull { !it.overview.isNullOrBlank() }?.overview
                ?: primary.overview
            val richestRating = group.mapNotNull { it.rating }.firstOrNull { it > 0.0 } ?: primary.rating
            val richestGenres = group.firstOrNull { !it.genres.isNullOrBlank() }?.genres ?: primary.genres
            val richestProviders = group.firstOrNull { !it.providerIds.isNullOrBlank() && it.providerIds != "none" }?.providerIds
                ?: primary.providerIds
            val richestReleaseDate = group.firstOrNull { !it.releaseDate.isNullOrBlank() }?.releaseDate ?: primary.releaseDate
            val canonicalTitle = if (primary.title.isNotBlank()) primary.title else group.first().title

            val allNotes = group.mapNotNull { it.userNotes?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            val mergedNotes = if (allNotes.isNotEmpty()) allNotes.joinToString(" • ").take(500) else primary.userNotes

            val allSources = group.mapNotNull { it.importSource?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            val mergedSource = if (allSources.isNotEmpty()) allSources.joinToString(", ").take(200) else primary.importSource

            val merged = primary.copy(
                title = canonicalTitle,
                status = if (isWatched) MediaStatus.WATCHED.name else primary.status,
                watchedAt = if (isWatched) (bestWatchedAt ?: primary.watchedAt ?: primary.addedAt) else primary.watchedAt,
                tmdbId = richestTmdbId,
                imageUrl = richestImage,
                overview = richestOverview,
                rating = richestRating,
                genres = richestGenres,
                providerIds = richestProviders,
                releaseDate = richestReleaseDate,
                userNotes = mergedNotes,
                importSource = mergedSource,
                updatedAt = System.currentTimeMillis()
            )

            return Pair(merged, losers.map { it.id })
        }

        // Pass 1: Deduplicate by non-blank TMDB ID
        val tmdbGroups = all.filter { !it.tmdbId.isNullOrBlank() && it.tmdbId != "0" }.groupBy { it.tmdbId!!.trim() }
        for ((_, group) in tmdbGroups) {
            if (group.size > 1) {
                val (merged, loserIds) = mergeGroup(group)
                itemsToUpdate.add(merged)
                idsToDelete.addAll(loserIds)
            }
        }

        // Pass 2: Deduplicate remaining items by normalized title
        val remaining = all.filter { !idsToDelete.contains(it.id) }
        val titleGroups = remaining.groupBy { com.example.data.remote.TmdbMatchingHelper.normalizeTitle(it.title) }
        for ((normTitle, group) in titleGroups) {
            if (normTitle.isNotBlank() && group.size > 1) {
                val (merged, loserIds) = mergeGroup(group)
                itemsToUpdate.add(merged)
                idsToDelete.addAll(loserIds)
            }
        }

        if (itemsToUpdate.isNotEmpty()) {
            updateMediaItems(itemsToUpdate)
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
