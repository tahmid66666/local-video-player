package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val uriString: String,
    val durationMs: Long = 0L,
    val lastPositionMs: Long = 0L,
    val lastPlayedTimestamp: Long = 0L,
    val resolution: String = "720p HD",
    val subtitleUri: String? = null,
    val isFavorite: Boolean = false,
    val orderIndex: Int = 0
)
