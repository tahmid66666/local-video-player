package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YouTubeBufferedTrack
import com.example.ui.theme.YouTubeRed
import com.example.util.TimeUtils
import kotlin.math.roundToInt

@Composable
fun DragToSeekProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeekStarted: () -> Unit,
    onSeekChanged: (Long) -> Unit,
    onSeekFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val effectiveProgress = if (isDragging) dragProgress else actualProgress

    val bufferedFraction = if (durationMs > 0) {
        (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 4.dp,
        label = "trackHeight"
    )
    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 6.dp,
        label = "thumbRadius"
    )

    val previewTimeMs = remember(effectiveProgress, durationMs) {
        (effectiveProgress * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp) // Accessible touch target
            .testTag("drag_to_seek_bar"),
        contentAlignment = Alignment.CenterStart
    ) {
        // Time balloon preview when dragging
        if (isDragging) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        // Offset preview balloon roughly according to thumb position
                        IntOffset(
                            x = 0,
                            y = -70
                        )
                    }
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xE6000000))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = TimeUtils.formatDuration(previewTimeMs),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerInput(durationMs) {
                    detectTapGestures { offset ->
                        if (durationMs > 0 && size.width > 0) {
                            val targetFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (targetFraction * durationMs).toLong()
                            onSeekStarted()
                            onSeekChanged(targetMs)
                            onSeekFinished(targetMs)
                        }
                    }
                }
                .pointerInput(durationMs) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (durationMs > 0 && size.width > 0) {
                                isDragging = true
                                dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeekStarted()
                                onSeekChanged((dragProgress * durationMs).toLong())
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (durationMs > 0 && size.width > 0) {
                                dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeekChanged((dragProgress * durationMs).toLong())
                            }
                        },
                        onDragEnd = {
                            if (durationMs > 0) {
                                isDragging = false
                                val targetMs = (dragProgress * durationMs).toLong()
                                onSeekFinished(targetMs)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val trackH = trackHeight.toPx()
            val thumbR = thumbRadius.toPx()

            // 1. Background inactive track
            drawRoundRect(
                color = Color(0x44FFFFFF),
                topLeft = Offset(0f, centerY - trackH / 2),
                size = Size(width, trackH),
                cornerRadius = CornerRadius(trackH / 2, trackH / 2)
            )

            // 2. Buffered progress track
            if (bufferedFraction > 0f) {
                drawRoundRect(
                    color = YouTubeBufferedTrack,
                    topLeft = Offset(0f, centerY - trackH / 2),
                    size = Size(width * bufferedFraction, trackH),
                    cornerRadius = CornerRadius(trackH / 2, trackH / 2)
                )
            }

            // 3. Played progress track (YouTube Red)
            val playedWidth = width * effectiveProgress
            drawRoundRect(
                color = YouTubeRed,
                topLeft = Offset(0f, centerY - trackH / 2),
                size = Size(playedWidth, trackH),
                cornerRadius = CornerRadius(trackH / 2, trackH / 2)
            )

            // 4. Scrubber thumb (Red circle with white inner ring or glow)
            drawCircle(
                color = YouTubeRed,
                radius = thumbR,
                center = Offset(playedWidth.coerceIn(thumbR, width - thumbR), centerY)
            )
        }
    }
}
