package com.example.data.repository

import com.example.data.local.MediaDao
import com.example.data.local.ProviderUsageStats
import com.example.data.model.MediaItem
import com.example.data.model.MediaStatus
import com.example.data.model.StreamingProvider
import com.example.data.model.WatchSession
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository layer acting as the single source of truth for all stream/watch data.
 * Adheres strictly to the Clean Architecture Repository Pattern.
 * 
 * FUTURE-PROOFING & CLOUD SYNC:
 * 1. User Authentication: This repository can easily accept a flow of UserID (e.g., from Firebase Auth) 
 *    and append `userId` fields to database calls.
 * 2. Cloud Syncing: We can insert a remote sync delegate (e.g., Firestore or GraphQL API client) that pushes
 *    local changes to a server database and listens for online updates to merge locally using Room.
 * 3. Multi-User/Social: Entities like WatchSession and MediaItem can reference owner `userId` or shared `groupIds` 
 *    for social watchlist comparisons and split-budget features.
 */
class MediaRepository(private val mediaDao: MediaDao) {

    // --- Media (Watchlist) Management ---

    val allMediaItems: Flow<List<MediaItem>> = mediaDao.getAllMediaItems()

    fun getMediaByStatus(status: MediaStatus): Flow<List<MediaItem>> {
        return mediaDao.getMediaItemsByStatus(status.name)
    }

    suspend fun getMediaItemById(id: Long): MediaItem? {
        return mediaDao.getMediaItemById(id)
    }

    suspend fun insertMediaItem(item: MediaItem): Long {
        return mediaDao.insertMediaItem(item)
    }

    suspend fun updateMediaItem(item: MediaItem) {
        mediaDao.updateMediaItem(item)
    }

    suspend fun deleteMediaItem(item: MediaItem) {
        mediaDao.deleteMediaItem(item)
    }

    suspend fun deleteMediaItemById(id: Long) {
        mediaDao.deleteMediaItemById(id)
    }

    suspend fun getIntendingToWatchItems(): List<MediaItem> {
        return mediaDao.getIntendingToWatchItems()
    }

    suspend fun getAllWatchSessionsList(): List<WatchSession> {
        return mediaDao.getAllWatchSessionsList()
    }


    // --- Streaming Providers Configuration ---

    val allStreamingProviders: Flow<List<StreamingProvider>> = mediaDao.getAllStreamingProvidersFl()
    
    val activeStreamingProviders: Flow<List<StreamingProvider>> = mediaDao.getActiveStreamingProvidersFl()

    suspend fun getActiveStreamingProvidersList(): List<StreamingProvider> {
        return mediaDao.getActiveStreamingProviders()
    }

    suspend fun updateStreamingProvider(provider: StreamingProvider) {
        mediaDao.insertStreamingProvider(provider)
    }

    suspend fun addStreamingProvider(provider: StreamingProvider) {
        mediaDao.insertStreamingProvider(provider)
    }

    suspend fun deleteStreamingProvider(providerId: String) {
        mediaDao.deleteStreamingProviderById(providerId)
    }


    // --- Sessions & Financial ROI Analyzers ---

    suspend fun addWatchSession(
        mediaItemId: Long,
        mediaItemTitle: String,
        providerId: String?,
        durationMinutes: Int,
        notes: String? = null
    ) {
        val session = WatchSession(
            mediaItemId = mediaItemId,
            mediaItemTitle = mediaItemTitle,
            providerId = providerId,
            durationMinutes = durationMinutes,
            notes = notes
        )
        // Insert watch session
        mediaDao.insertWatchSession(session)

        // Mark corresponding MediaItem as WATCHED in database historical record
        val mediaItem = mediaDao.getMediaItemById(mediaItemId)
        if (mediaItem != null) {
            mediaDao.updateMediaItem(
                mediaItem.copy(
                    status = MediaStatus.WATCHED.name,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Retrieves current month's subscription value analytics.
     * Calculated using total hours grouped by the designated providerId.
     */
    fun getCurrentMonthUsageStats(): Flow<List<ProviderUsageStats>> {
        val calendar = Calendar.getInstance()
        
        // Start of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTimestamp = calendar.timeInMillis

        // End of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTimestamp = calendar.timeInMillis

        return mediaDao.getMonthlyUsageStats(startTimestamp, endTimestamp)
    }

    suspend fun getWatchedMediaItemsList(): List<MediaItem> {
        return mediaDao.getWatchedMediaItemsList()
    }

    suspend fun getUnsyncedMediaItems(): List<MediaItem> {
        return mediaDao.getUnsyncedMediaItems()
    }

    suspend fun markItemsSynced(items: List<MediaItem>) {
        items.forEach { item ->
            mediaDao.updateMediaItem(item.copy(syncedToSheet = true))
        }
    }

    suspend fun getMediaItemByTitle(title: String): MediaItem? {
        return mediaDao.getMediaItemByTitle(title)
    }

    suspend fun logWatchedMovieDirectly(
        title: String,
        year: String? = null,
        userRating: Double? = null,
        isRewatch: Boolean = false,
        providerId: String? = null,
        durationMinutes: Int = 120,
        notes: String? = null,
        letterboxdUri: String? = null
    ): MediaItem {
        val now = System.currentTimeMillis()
        val existing = mediaDao.getMediaItemByTitle(title)
        val savedItem: MediaItem

        if (existing != null) {
            savedItem = existing.copy(
                status = MediaStatus.WATCHED.name,
                watchedAt = now,
                updatedAt = now,
                userRating = userRating ?: existing.userRating,
                isRewatch = isRewatch,
                userNotes = notes ?: existing.userNotes,
                releaseYear = year ?: existing.releaseYear,
                runtimeMinutes = durationMinutes,
                letterboxdUri = letterboxdUri ?: existing.letterboxdUri,
                syncedToSheet = false
            )
            mediaDao.updateMediaItem(savedItem)
        } else {
            val newItem = MediaItem(
                title = title,
                status = MediaStatus.WATCHED.name,
                addedAt = now,
                watchedAt = now,
                updatedAt = now,
                userRating = userRating,
                isRewatch = isRewatch,
                userNotes = notes,
                releaseYear = year,
                runtimeMinutes = durationMinutes,
                letterboxdUri = letterboxdUri,
                syncedToSheet = false
            )
            val newId = mediaDao.insertMediaItem(newItem)
            savedItem = newItem.copy(id = newId)
        }

        // Add watch session for monthly subscription ROI calculation
        val session = WatchSession(
            mediaItemId = savedItem.id,
            mediaItemTitle = savedItem.title,
            providerId = providerId,
            durationMinutes = durationMinutes,
            notes = notes ?: "Logged via Streamwise Quick-Log"
        )
        mediaDao.insertWatchSession(session)
        return savedItem
    }
}
