package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY orderIndex ASC, lastPlayedTimestamp DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoById(id: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET lastPositionMs = :positionMs, lastPlayedTimestamp = :timestamp WHERE id = :id")
    suspend fun updatePlaybackPosition(id: String, positionMs: Long, timestamp: Long)

    @Query("UPDATE videos SET durationMs = :durationMs WHERE id = :id")
    suspend fun updateDuration(id: String, durationMs: Long)

    @Query("UPDATE videos SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String)

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int
}
