package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YouTubeRed
import kotlin.math.roundToInt

@Composable
fun GestureHud(
    brightnessPercent: Float?,
    volumePercent: Float?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Brightness HUD
        AnimatedVisibility(
            visible = brightnessPercent != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f)
        ) {
            brightnessPercent?.let { level ->
                val icon = when {
                    level > 0.66f -> Icons.Default.BrightnessHigh
                    level > 0.33f -> Icons.Default.BrightnessMedium
                    else -> Icons.Default.BrightnessLow
                }
                HudCard(
                    title = "Brightness",
                    percent = (level * 100).roundToInt(),
                    icon = icon,
                    modifier = Modifier.testTag("brightness_hud")
                )
            }
        }

        // Volume HUD
        AnimatedVisibility(
            visible = volumePercent != null,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f)
        ) {
            volumePercent?.let { level ->
                val icon = when {
                    level > 0.66f -> Icons.Default.VolumeUp
                    level > 0.05f -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeMute
                }
                HudCard(
                    title = "Volume",
                    percent = (level * 100).roundToInt(),
                    icon = icon,
                    modifier = Modifier.testTag("volume_hud")
                )
            }
        }
    }
}

@Composable
private fun HudCard(
    title: String,
    percent: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC111115))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$title $percent%",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0x44FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = (percent / 100f).coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(YouTubeRed)
                )
            }
        }
    }
}
