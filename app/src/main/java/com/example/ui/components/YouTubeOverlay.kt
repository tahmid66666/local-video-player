package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.RepeatMode
import com.example.ui.theme.YouTubeBlue
import com.example.ui.theme.YouTubeOverlayScrim
import com.example.ui.theme.YouTubeRed
import com.example.util.TimeUtils

@Composable
fun YouTubeOverlay(
    isVisible: Boolean,
    isPlaying: Boolean,
    title: String,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    playbackSpeed: Float,
    repeatMode: RepeatMode,
    isShuffleEnabled: Boolean,
    isSubtitlesEnabled: Boolean,
    isLandscape: Boolean,
    onPlayPauseClick: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeekStarted: () -> Unit,
    onSeekChanged: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitlesToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onShuffleToggle: () -> Unit,
    onLockClick: () -> Unit,
    onPipClick: () -> Unit,
    onOrientationToggle: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(YouTubeOverlayScrim)
                .testTag("youtube_player_overlay")
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOrientationToggle,
                    modifier = Modifier.testTag("collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize or toggle orientation",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                // Subtitle CC Button
                IconButton(
                    onClick = onSubtitlesToggle,
                    modifier = Modifier.testTag("subtitles_toggle_button")
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSubtitlesEnabled) YouTubeRed else Color.Transparent)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isSubtitlesEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                            contentDescription = "Toggle Subtitles",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Playback speed chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable { onSpeedClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("speed_chip_button")
                ) {
                    Text(
                        text = if (playbackSpeed == 1.0f) "1x" else "${playbackSpeed}x",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // PiP Button
                IconButton(
                    onClick = onPipClick,
                    modifier = Modifier.testTag("pip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Picture in Picture",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Details / Settings button
                IconButton(
                    onClick = onDetailsClick,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Video Details",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center Controls: Prev, Rewind 10s, Play/Pause, Forward 10s, Next
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous video",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Rewind 10s
                IconButton(
                    onClick = onRewind10,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("rewind_10_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Big Center Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                        .clickable { onPlayPauseClick() }
                        .testTag("center_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Fast Forward 10s
                IconButton(
                    onClick = onForward10,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("forward_10_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10, // will be mirrored / standard forward
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next video",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            // Bottom Bar: Progress Bar & Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Drag-to-seek scrubber
                DragToSeekProgressBar(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    bufferedPositionMs = bufferedPositionMs,
                    onSeekStarted = onSeekStarted,
                    onSeekChanged = onSeekChanged,
                    onSeekFinished = onSeekFinished
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time text
                    Text(
                        text = "${TimeUtils.formatDuration(currentPositionMs)} / ${TimeUtils.formatDuration(durationMs)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("time_display")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle toggle
                        IconButton(
                            onClick = onShuffleToggle,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffleEnabled) YouTubeBlue else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Repeat toggle
                        IconButton(
                            onClick = onRepeatToggle,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("repeat_button")
                        ) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    RepeatMode.OFF -> Icons.Default.Repeat
                                    RepeatMode.ALL -> Icons.Default.Repeat
                                    RepeatMode.ONE -> Icons.Default.RepeatOne
                                },
                                contentDescription = "Repeat mode",
                                tint = if (repeatMode != RepeatMode.OFF) YouTubeBlue else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Lock Screen button
                        IconButton(
                            onClick = onLockClick,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("lock_screen_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Controls",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Orientation / Fullscreen toggle
                        IconButton(
                            onClick = onOrientationToggle,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("fullscreen_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
