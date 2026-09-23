package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.model.VideoEntity
import com.example.data.repository.VideoRepository
import com.example.util.SubtitleCue
import com.example.util.SubtitleParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class SeekDirection {
    BACKWARD, FORWARD
}

data class SeekFeedback(
    val direction: SeekDirection,
    val seconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    val videos: StateFlow<List<VideoEntity>>

    private val _currentVideo = MutableStateFlow<VideoEntity?>(null)
    val currentVideo: StateFlow<VideoEntity?> = _currentVideo.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _bufferedPositionMs = MutableStateFlow(0L)
    val bufferedPositionMs: StateFlow<Long> = _bufferedPositionMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    private val _isScreenLocked = MutableStateFlow(false)
    val isScreenLocked: StateFlow<Boolean> = _isScreenLocked.asStateFlow()

    private val _isSubtitlesEnabled = MutableStateFlow(true)
    val isSubtitlesEnabled: StateFlow<Boolean> = _isSubtitlesEnabled.asStateFlow()

    private val _currentSubtitleText = MutableStateFlow<String?>(null)
    val currentSubtitleText: StateFlow<String?> = _currentSubtitleText.asStateFlow()

    private var currentSubtitleCues = listOf<SubtitleCue>()

    // Gesture HUD states (null when dismissed, 0.0 - 1.0 when active)
    private val _brightnessPercent = MutableStateFlow<Float?>(null)
    val brightnessPercent: StateFlow<Float?> = _brightnessPercent.asStateFlow()

    private val _volumePercent = MutableStateFlow<Float?>(null)
    val volumePercent: StateFlow<Float?> = _volumePercent.asStateFlow()

    private val _seekFeedback = MutableStateFlow<SeekFeedback?>(null)
    val seekFeedback: StateFlow<SeekFeedback?> = _seekFeedback.asStateFlow()

    // Sheet / Dialogs
    private val _isSpeedSheetVisible = MutableStateFlow(false)
    val isSpeedSheetVisible: StateFlow<Boolean> = _isSpeedSheetVisible.asStateFlow()

    private val _isDetailsDialogVisible = MutableStateFlow(false)
    val isDetailsDialogVisible: StateFlow<Boolean> = _isDetailsDialogVisible.asStateFlow()

    private val _isLandscape = MutableStateFlow(false)
    val isLandscape: StateFlow<Boolean> = _isLandscape.asStateFlow()

    private var autoHideControlsJob: Job? = null
    private var dismissHudJob: Job? = null
    private var dismissSeekFeedbackJob: Job? = null

    init {
        val database = AppDatabase.getInstance(application)
        repository = VideoRepository(application, database.videoDao())
        videos = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.initializeDefaultVideosIfEmpty()
            videos.collect { list ->
                if (_currentVideo.value == null && list.isNotEmpty()) {
                    selectVideo(list.first())
                }
            }
        }
    }

    fun selectVideo(video: VideoEntity) {
        _currentVideo.value = video
        _playbackPositionMs.value = video.lastPositionMs
        _currentSubtitleText.value = null

        // Load subtitles if available
        loadSubtitlesForVideo(video)
        showControls()
    }

    private fun loadSubtitlesForVideo(video: VideoEntity) {
        val context = getApplication<Application>()
        if (video.subtitleUri != null) {
            if (video.subtitleUri.contains("subtitles_cosmic")) {
                currentSubtitleCues = SubtitleParser.loadSampleSubtitle(context, R.raw.subtitles_cosmic)
            } else if (video.subtitleUri.contains("subtitles_neon")) {
                currentSubtitleCues = SubtitleParser.loadSampleSubtitle(context, R.raw.subtitles_neon)
            } else {
                try {
                    val uri = Uri.parse(video.subtitleUri)
                    currentSubtitleCues = SubtitleParser.loadSubtitleFromUri(context, uri)
                } catch (e: Exception) {
                    currentSubtitleCues = emptyList()
                }
            }
        } else {
            currentSubtitleCues = emptyList()
        }
    }

    fun loadExternalSubtitle(uri: Uri) {
        val context = getApplication<Application>()
        val cues = SubtitleParser.loadSubtitleFromUri(context, uri)
        if (cues.isNotEmpty()) {
            currentSubtitleCues = cues
            _isSubtitlesEnabled.value = true
            val video = _currentVideo.value
            if (video != null) {
                viewModelScope.launch {
                    val updated = video.copy(subtitleUri = uri.toString())
                    repository.addVideo(updated)
                    _currentVideo.value = updated
                }
            }
        }
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) {
            scheduleAutoHideControls()
        } else {
            autoHideControlsJob?.cancel()
            _isControlsVisible.value = true
        }
    }

    fun updateProgress(currentMs: Long, durationMs: Long, bufferedMs: Long) {
        _playbackPositionMs.value = currentMs
        if (durationMs > 0) {
            _durationMs.value = durationMs
        }
        _bufferedPositionMs.value = bufferedMs

        // Update active subtitle
        if (_isSubtitlesEnabled.value && currentSubtitleCues.isNotEmpty()) {
            _currentSubtitleText.value = SubtitleParser.getActiveSubtitle(currentSubtitleCues, currentMs)
        } else {
            _currentSubtitleText.value = null
        }

        // Periodically persist playback position to database
        val video = _currentVideo.value
        if (video != null && currentMs > 0 && Math.abs(currentMs - video.lastPositionMs) > 2000) {
            viewModelScope.launch {
                repository.updatePlaybackPosition(video.id, currentMs)
                if (durationMs > 0 && video.durationMs <= 0) {
                    repository.updateDuration(video.id, durationMs)
                }
            }
        }
    }

    fun togglePlayPause(): Boolean {
        val newState = !_isPlaying.value
        _isPlaying.value = newState
        showControls()
        return newState
    }

    fun toggleControls() {
        if (_isScreenLocked.value) {
            // If screen locked, flash the lock pill/button
            _isControlsVisible.value = true
            scheduleAutoHideControls(2500)
            return
        }
        val target = !_isControlsVisible.value
        _isControlsVisible.value = target
        if (target && _isPlaying.value) {
            scheduleAutoHideControls()
        }
    }

    fun showControls() {
        _isControlsVisible.value = true
        if (_isPlaying.value && !_isScreenLocked.value) {
            scheduleAutoHideControls()
        }
    }

    private fun scheduleAutoHideControls(timeoutMs: Long = 4000) {
        autoHideControlsJob?.cancel()
        autoHideControlsJob = viewModelScope.launch {
            delay(timeoutMs)
            _isControlsVisible.value = false
        }
    }

    fun cancelAutoHide() {
        autoHideControlsJob?.cancel()
    }

    fun toggleScreenLock() {
        val locked = !_isScreenLocked.value
        _isScreenLocked.value = locked
        if (locked) {
            _isControlsVisible.value = false
        } else {
            showControls()
        }
    }

    fun unlockScreen() {
        _isScreenLocked.value = false
        showControls()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        _isSpeedSheetVisible.value = false
    }

    fun openSpeedSheet() {
        _isSpeedSheetVisible.value = true
    }

    fun closeSpeedSheet() {
        _isSpeedSheetVisible.value = false
    }

    fun openDetailsDialog() {
        _isDetailsDialogVisible.value = true
    }

    fun closeDetailsDialog() {
        _isDetailsDialogVisible.value = false
    }

    fun toggleSubtitles() {
        _isSubtitlesEnabled.value = !_isSubtitlesEnabled.value
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun setLandscape(landscape: Boolean) {
        _isLandscape.value = landscape
    }

    fun toggleOrientation(): Boolean {
        val newMode = !_isLandscape.value
        _isLandscape.value = newMode
        return newMode
    }

    fun onDoubleTapSeek(direction: SeekDirection, currentPos: Long, totalDuration: Long): Long {
        val delta = if (direction == SeekDirection.FORWARD) 10000L else -10000L
        val newPos = (currentPos + delta).coerceIn(0L, totalDuration.coerceAtLeast(0L))

        _seekFeedback.value = SeekFeedback(direction, 10)
        dismissSeekFeedbackJob?.cancel()
        dismissSeekFeedbackJob = viewModelScope.launch {
            delay(800)
            _seekFeedback.value = null
        }
        return newPos
    }

    fun adjustBrightness(delta: Float, windowManager: WindowManager?, windowAttributes: WindowManager.LayoutParams?) {
        if (_isScreenLocked.value) return
        val current = windowAttributes?.screenBrightness ?: 0.5f
        val effectiveCurrent = if (current < 0f) 0.5f else current
        val newBrightness = (effectiveCurrent + delta).coerceIn(0.01f, 1.0f)
        windowAttributes?.screenBrightness = newBrightness
        _brightnessPercent.value = newBrightness

        scheduleDismissHud()
    }

    fun adjustVolume(delta: Float, audioManager: AudioManager) {
        if (_isScreenLocked.value) return
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val currentRatio = currentVolume.toFloat() / maxVolume.toFloat()

        val newRatio = (currentRatio + delta).coerceIn(0.0f, 1.0f)
        val newVolume = (newRatio * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        _volumePercent.value = newRatio

        scheduleDismissHud()
    }

    private fun scheduleDismissHud() {
        dismissHudJob?.cancel()
        dismissHudJob = viewModelScope.launch {
            delay(1200)
            _brightnessPercent.value = null
            _volumePercent.value = null
        }
    }

    fun playNextVideo(): VideoEntity? {
        val list = videos.value
        if (list.isEmpty()) return null
        val current = _currentVideo.value ?: return list.first()
        val currentIndex = list.indexOfFirst { it.id == current.id }

        val nextIndex = if (_isShuffleEnabled.value) {
            val remaining = list.indices.filter { it != currentIndex }
            if (remaining.isNotEmpty()) remaining.random() else currentIndex
        } else {
            (currentIndex + 1) % list.size
        }

        val next = list[nextIndex]
        selectVideo(next)
        return next
    }

    fun playPreviousVideo(): VideoEntity? {
        val list = videos.value
        if (list.isEmpty()) return null
        val current = _currentVideo.value ?: return list.first()
        val currentIndex = list.indexOfFirst { it.id == current.id }

        val prevIndex = if (_isShuffleEnabled.value) {
            val remaining = list.indices.filter { it != currentIndex }
            if (remaining.isNotEmpty()) remaining.random() else currentIndex
        } else {
            if (currentIndex <= 0) list.size - 1 else currentIndex - 1
        }

        val prev = list[prevIndex]
        selectVideo(prev)
        return prev
    }

    fun addImportedVideo(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            var fileName = "Imported Video"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                fileName = uri.lastPathSegment ?: "Imported Video"
            }

            val newVideo = VideoEntity(
                id = uri.toString(),
                title = fileName,
                uriString = uri.toString(),
                durationMs = 0L,
                resolution = "Local Media",
                subtitleUri = null,
                lastPlayedTimestamp = System.currentTimeMillis(),
                orderIndex = (videos.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
            )
            repository.addVideo(newVideo)
            selectVideo(newVideo)
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
            if (_currentVideo.value?.id == id) {
                _currentVideo.value = _currentVideo.value?.copy(
                    isFavorite = !(_currentVideo.value?.isFavorite ?: false)
                )
            }
        }
    }

    fun deleteVideo(id: String) {
        viewModelScope.launch {
            repository.deleteVideo(id)
            if (_currentVideo.value?.id == id) {
                val remaining = videos.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectVideo(remaining.first())
                } else {
                    _currentVideo.value = null
                }
            }
        }
    }
}
