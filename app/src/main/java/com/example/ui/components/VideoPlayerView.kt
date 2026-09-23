package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.ui.PlayerViewModel
import com.example.ui.RepeatMode
import com.example.ui.SeekDirection
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    viewModel: PlayerViewModel,
    onOrientationToggle: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val currentVideo by viewModel.currentVideo.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.playbackPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val bufferedPositionMs by viewModel.bufferedPositionMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isControlsVisible by viewModel.isControlsVisible.collectAsStateWithLifecycle()
    val isScreenLocked by viewModel.isScreenLocked.collectAsStateWithLifecycle()
    val isSubtitlesEnabled by viewModel.isSubtitlesEnabled.collectAsStateWithLifecycle()
    val currentSubtitleText by viewModel.currentSubtitleText.collectAsStateWithLifecycle()
    val brightnessPercent by viewModel.brightnessPercent.collectAsStateWithLifecycle()
    val volumePercent by viewModel.volumePercent.collectAsStateWithLifecycle()
    val seekFeedback by viewModel.seekFeedback.collectAsStateWithLifecycle()
    val isLandscape by viewModel.isLandscape.collectAsStateWithLifecycle()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Handle repeat mode on ExoPlayer
    LaunchedEffect(repeatMode) {
        exoPlayer.repeatMode = when (repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    // Handle playback speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    // Handle video change
    LaunchedEffect(currentVideo?.uriString) {
        currentVideo?.let { video ->
            try {
                val uri = Uri.parse(video.uriString)
                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                if (video.lastPositionMs > 0) {
                    exoPlayer.seekTo(video.lastPositionMs)
                }
                exoPlayer.playWhenReady = true
                viewModel.onPlaybackStateChanged(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Sync play/pause state from ViewModel to ExoPlayer
    LaunchedEffect(isPlaying) {
        if (exoPlayer.isPlaying != isPlaying) {
            if (isPlaying) {
                exoPlayer.play()
            } else {
                exoPlayer.pause()
            }
        }
    }

    // Periodic progress updates
    LaunchedEffect(isPlaying, exoPlayer) {
        while (true) {
            if (exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING) {
                val current = exoPlayer.currentPosition.coerceAtLeast(0L)
                val duration = exoPlayer.duration.coerceAtLeast(0L)
                val buffered = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                viewModel.updateProgress(current, duration, buffered)
            }
            delay(250)
        }
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                viewModel.onPlaybackStateChanged(playing)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    when (repeatMode) {
                        RepeatMode.ONE -> {
                            exoPlayer.seekTo(0)
                            exoPlayer.play()
                        }
                        RepeatMode.ALL, RepeatMode.OFF -> {
                            val next = viewModel.playNextVideo()
                            if (next == null && repeatMode == RepeatMode.OFF) {
                                viewModel.onPlaybackStateChanged(false)
                            }
                        }
                    }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    var dragStartX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .background(Color.Black)
            .testTag("video_player_container")
            // Tap gestures: Double-tap seek & single tap toggle overlay
            .pointerInput(isScreenLocked) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isScreenLocked) {
                            val width = size.width
                            if (offset.x < width * 0.35f) {
                                val newPos = viewModel.onDoubleTapSeek(
                                    SeekDirection.BACKWARD,
                                    exoPlayer.currentPosition,
                                    exoPlayer.duration
                                )
                                exoPlayer.seekTo(newPos)
                            } else if (offset.x > width * 0.65f) {
                                val newPos = viewModel.onDoubleTapSeek(
                                    SeekDirection.FORWARD,
                                    exoPlayer.currentPosition,
                                    exoPlayer.duration
                                )
                                exoPlayer.seekTo(newPos)
                            } else {
                                val newPlayState = viewModel.togglePlayPause()
                                if (newPlayState) exoPlayer.play() else exoPlayer.pause()
                            }
                        }
                    },
                    onTap = {
                        viewModel.toggleControls()
                    }
                )
            }
            // Vertical Drag gestures: Brightness (left side) and Volume (right side)
            .pointerInput(isScreenLocked) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!isScreenLocked && size.height > 0) {
                            val deltaFraction = -dragAmount.y / (size.height * 0.65f)
                            if (dragStartX < size.width * 0.5f) {
                                // Left side: Brightness
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    viewModel.adjustBrightness(deltaFraction, act.windowManager, lp)
                                    act.window.attributes = lp
                                }
                            } else {
                                // Right side: Volume
                                viewModel.adjustVolume(deltaFraction, audioManager)
                            }
                        }
                    }
                )
            }
    ) {
        // ExoPlayer Native Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = exoPlayer
                    this.useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtitles Overlay
        SubtitleOverlay(
            text = if (isSubtitlesEnabled) currentSubtitleText else null,
            isControlsVisible = isControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // YouTube Controls Overlay
        if (!isScreenLocked) {
            YouTubeOverlay(
                isVisible = isControlsVisible,
                isPlaying = isPlaying,
                title = currentVideo?.title ?: "Offline Video",
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                bufferedPositionMs = bufferedPositionMs,
                playbackSpeed = playbackSpeed,
                repeatMode = repeatMode,
                isShuffleEnabled = isShuffleEnabled,
                isSubtitlesEnabled = isSubtitlesEnabled,
                isLandscape = isLandscape,
                onPlayPauseClick = {
                    val nextPlay = viewModel.togglePlayPause()
                    if (nextPlay) exoPlayer.play() else exoPlayer.pause()
                },
                onRewind10 = {
                    val newPos = viewModel.onDoubleTapSeek(
                        SeekDirection.BACKWARD,
                        exoPlayer.currentPosition,
                        exoPlayer.duration
                    )
                    exoPlayer.seekTo(newPos)
                },
                onForward10 = {
                    val newPos = viewModel.onDoubleTapSeek(
                        SeekDirection.FORWARD,
                        exoPlayer.currentPosition,
                        exoPlayer.duration
                    )
                    exoPlayer.seekTo(newPos)
                },
                onPreviousClick = {
                    viewModel.playPreviousVideo()
                },
                onNextClick = {
                    viewModel.playNextVideo()
                },
                onSeekStarted = {
                    viewModel.cancelAutoHide()
                },
                onSeekChanged = { seekMs ->
                    exoPlayer.seekTo(seekMs)
                    viewModel.updateProgress(seekMs, durationMs, bufferedPositionMs)
                },
                onSeekFinished = { finalMs ->
                    exoPlayer.seekTo(finalMs)
                    viewModel.showControls()
                },
                onSpeedClick = {
                    viewModel.openSpeedSheet()
                },
                onSubtitlesToggle = {
                    viewModel.toggleSubtitles()
                },
                onRepeatToggle = {
                    viewModel.toggleRepeatMode()
                },
                onShuffleToggle = {
                    viewModel.toggleShuffle()
                },
                onLockClick = {
                    viewModel.toggleScreenLock()
                },
                onPipClick = onPipClick,
                onOrientationToggle = onOrientationToggle,
                onDetailsClick = {
                    viewModel.openDetailsDialog()
                }
            )
        }

        // Screen Lock Overlay (when screen is locked)
        ScreenLockOverlay(
            isLocked = isScreenLocked,
            isPromptVisible = isControlsVisible,
            onUnlock = { viewModel.unlockScreen() }
        )

        // Double Tap Ripple Seek Feedback (YouTube style +10s / -10s animation)
        DoubleTapSeekFeedback(
            seekFeedback = seekFeedback,
            modifier = Modifier.fillMaxSize()
        )

        // Brightness & Volume Floating HUD
        GestureHud(
            brightnessPercent = brightnessPercent,
            volumePercent = volumePercent,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
