package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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

    suspend fun scanDeviceVideos(): Int = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoEntity>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleColumn = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)

                var index = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val displayName = if (nameColumn != -1) cursor.getString(nameColumn) else null
                    val title = if (titleColumn != -1) cursor.getString(titleColumn) else null
                    val durationMs = if (durationColumn != -1) cursor.getLong(durationColumn) else 0L
                    val width = if (widthColumn != -1) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn != -1) cursor.getInt(heightColumn) else 0

                    val finalTitle = title?.takeIf { it.isNotBlank() } ?: displayName ?: "Video $id"
                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else "Local Video"

                    videoList.add(
                        VideoEntity(
                            id = contentUri.toString(),
                            title = finalTitle,
                            uriString = contentUri.toString(),
                            durationMs = durationMs,
                            resolution = resolution,
                            subtitleUri = null,
                            orderIndex = index++
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (videoList.isNotEmpty()) {
            videoDao.insertAll(videoList)
        }
        videoList.size
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
