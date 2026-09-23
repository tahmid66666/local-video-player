package com.example.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.util.Rational
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.PlaylistQueueView
import com.example.ui.components.SpeedBottomSheet
import com.example.ui.components.VideoDetailsDialog
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.YouTubeDarkBg

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current

    val currentVideo by viewModel.currentVideo.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isSubtitlesEnabled by viewModel.isSubtitlesEnabled.collectAsStateWithLifecycle()
    val isSpeedSheetVisible by viewModel.isSpeedSheetVisible.collectAsStateWithLifecycle()
    val isDetailsDialogVisible by viewModel.isDetailsDialogVisible.collectAsStateWithLifecycle()

    val isDeviceLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isViewModelLandscape by viewModel.isLandscape.collectAsStateWithLifecycle()

    // Sync physical device orientation
    LaunchedEffect(isDeviceLandscape) {
        viewModel.setLandscape(isDeviceLandscape)
    }

    val isLandscape = isDeviceLandscape || isViewModelLandscape

    val onToggleOrientation = {
        val nextIsLandscape = viewModel.toggleOrientation()
        activity?.requestedOrientation = if (nextIsLandscape) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val onEnterPip = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                activity?.enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeDarkBg)
    ) {
        if (isLandscape) {
            // Fullscreen Cinematic Landscape Layout
            VideoPlayerView(
                viewModel = viewModel,
                onOrientationToggle = onToggleOrientation,
                onPipClick = { onEnterPip() },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Portrait Layout: Video Player on top (16:9), Playlist/Info below
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top 16:9 YouTube Video Player
                VideoPlayerView(
                    viewModel = viewModel,
                    onOrientationToggle = onToggleOrientation,
                    onPipClick = { onEnterPip() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )

                // Playlist Queue & Controls below player
                PlaylistQueueView(
                    viewModel = viewModel,
                    videos = videos,
                    currentVideo = currentVideo,
                    playbackSpeed = playbackSpeed,
                    isSubtitlesEnabled = isSubtitlesEnabled,
                    repeatMode = repeatMode,
                    isShuffleEnabled = isShuffleEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }

        // Playback Speed Bottom Sheet
        if (isSpeedSheetVisible) {
            SpeedBottomSheet(
                currentSpeed = playbackSpeed,
                onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
                onDismiss = { viewModel.closeSpeedSheet() }
            )
        }

        // Video Details Dialog
        if (isDetailsDialogVisible) {
            VideoDetailsDialog(
                video = currentVideo,
                currentSpeed = playbackSpeed,
                isSubtitlesOn = isSubtitlesEnabled,
                onDismiss = { viewModel.closeDetailsDialog() }
            )
        }
    }
}
