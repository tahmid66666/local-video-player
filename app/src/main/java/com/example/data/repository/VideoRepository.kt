package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.dao.VideoDao
import com.example.data.model.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VideoRepository(
    private val context: Context,
    private val videoDao: VideoDao
) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()

    suspend fun initializeDefaultVideosIfEmpty() = withContext(Dispatchers.IO) {
        val count = videoDao.getVideoCount()
        if (count == 0) {
            val pkg = context.packageName
            val sampleVideos = listOf(
                VideoEntity(
                    id = "sample_cosmic",
                    title = "Cosmic Odyssey (Offline Demo)",
                    uriString = "android.resource://$pkg/${R.raw.sample_cosmic}",
                    durationMs = 18000L,
                    resolution = "720p HD",
                    subtitleUri = "raw/subtitles_cosmic.srt",
                    orderIndex = 0
                ),
                VideoEntity(
                    id = "sample_neon",
                    title = "Neon Horizon (Synthwave Loop)",
                    uriString = "android.resource://$pkg/${R.raw.sample_neon}",
                    durationMs = 15000L,
                    resolution = "720p HD",
                    subtitleUri = "raw/subtitles_neon.srt",
                    orderIndex = 1
                ),
                VideoEntity(
                    id = "sample_test",
                    title = "Broadcast Test Pattern & Tone",
                    uriString = "android.resource://$pkg/${R.raw.sample_test}",
                    durationMs = 12000L,
                    resolution = "720p Standard",
                    subtitleUri = null,
                    orderIndex = 2
                )
            )
            videoDao.insertAll(sampleVideos)
        }
    }

    suspend fun addVideo(video: VideoEntity) = withContext(Dispatchers.IO) {
        videoDao.insertVideo(video)
    }

    suspend fun updatePlaybackPosition(id: String, positionMs: Long) = withContext(Dispatchers.IO) {
        videoDao.updatePlaybackPosition(id, positionMs, System.currentTimeMillis())
    }

    suspend fun updateDuration(id: String, durationMs: Long) = withContext(Dispatchers.IO) {
        videoDao.updateDuration(id, durationMs)
    }

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        videoDao.toggleFavorite(id)
    }

    suspend fun deleteVideo(id: String) = withContext(Dispatchers.IO) {
        videoDao.deleteVideo(id)
    }
}
