package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watch_sessions",
    foreignKeys = [
        ForeignKey(
            entity = MediaItem::class,
            parentColumns = ["id"],
            childColumns = ["mediaItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("mediaItemId"),
        Index("providerId")
    ]
)
data class WatchSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaItemId: Long,
    val mediaItemTitle: String,
    val providerId: String?, // Links to StreamingProvider.id, e.g., "netflix". Nullable if not watched on a subscription provider.
    val watchedAt: Long = System.currentTimeMillis(),
    val durationMinutes: Int, // Captured to calculate exact monthly subscription ROI
    val notes: String? = null
)
