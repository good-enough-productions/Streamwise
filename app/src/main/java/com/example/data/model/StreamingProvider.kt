package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaming_providers")
data class StreamingProvider(
    @PrimaryKey val id: String, // e.g. "netflix", "hulu", "max", "disney"
    val name: String,
    val logoUrl: String? = null,
    val costPerMonth: Double = 0.0,
    val isActive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
