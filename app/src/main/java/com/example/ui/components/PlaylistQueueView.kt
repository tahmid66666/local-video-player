package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VideoEntity
import com.example.ui.PlayerViewModel
import com.example.ui.RepeatMode
import com.example.ui.theme.YouTubeBlue
import com.example.ui.theme.YouTubeDarkBorder
import com.example.ui.theme.YouTubeDarkCard
import com.example.ui.theme.YouTubeDarkSurface
import com.example.ui.theme.YouTubeRed
import com.example.util.TimeUtils

@Composable
fun PlaylistQueueView(
    viewModel: PlayerViewModel,
    videos: List<VideoEntity>,
    currentVideo: VideoEntity?,
    playbackSpeed: Float,
    isSubtitlesEnabled: Boolean,
    repeatMode: RepeatMode,
    isShuffleEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanMessage.collectAsStateWithLifecycle()

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            viewModel.scanDeviceVideos()
        }
    }

    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            viewModel.scanDeviceVideos()
        }
    }

    // Media picker for picking any local video from device storage
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImportedVideo(it) }
    }

    // Subtitle picker for loading external SRT subtitle file
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadExternalSubtitle(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .background(YouTubeDarkSurface)
            .testTag("playlist_queue_view")
    ) {
        // Video Header Info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Title
                Text(
                    text = currentVideo?.title ?: "Select a Video",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Metadata Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Offline ready",
                        tint = YouTubeBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Offline Storage • ${currentVideo?.resolution ?: "720p"} • No Internet Needed",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Chips Row (YouTube style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Scan Device Videos Button
                    Button(
                        onClick = {
                            if (hasStoragePermission) {
                                viewModel.scanDeviceVideos()
                            } else {
                                permissionLauncher.launch(permissionToRequest)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) YouTubeBlue else Color(0x33FFFFFF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("scan_device_videos_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isScanning) "Scanning…" else "Scan Device",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Import Video Button
                    Button(
                        onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x33FFFFFF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("import_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Load Subtitles Button
                    Button(
                        onClick = { subtitlePickerLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x33FFFFFF),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("load_subtitles_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(".SRT", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Favorite Button
                    currentVideo?.let { vid ->
                        IconButton(
                            onClick = { viewModel.toggleFavorite(vid.id) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (vid.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (vid.isFavorite) YouTubeRed else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = YouTubeDarkBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Permission Request Banner if permission not granted
        if (!hasStoragePermission) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("storage_permission_card"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFF1E2638)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = YouTubeBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Local Video Access Needed",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Grant storage permission to allow the player to discover, index, and play local videos stored on your device.",
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { permissionLauncher.launch(permissionToRequest) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = YouTubeBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("grant_permission_button")
                            ) {
                                Text("Allow Access", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("open_settings_button")
                            ) {
                                Text("Settings", fontSize = 13.sp, color = Color(0xFFAAAAAA))
                            }
                        }
                    }
                }
            }
        }

        // Scan Status Banner
        scanMessage?.let { msg ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(Color(0xFF263238), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = msg, color = Color(0xFF81D4FA), fontSize = 12.sp)
                    IconButton(
                        onClick = { viewModel.dismissScanMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Section Title: Playlist / Up Next
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = YouTubeRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline Queue (${videos.size})",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isShuffleEnabled) "Shuffle: ON" else "Sequential",
                    color = if (isShuffleEnabled) YouTubeBlue else Color(0xFF888888),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // List of videos
        itemsIndexed(videos, key = { _, item -> item.id }) { index, video ->
            val isCurrent = video.id == currentVideo?.id
            VideoListItem(
                video = video,
                isCurrent = isCurrent,
                onClick = { viewModel.selectVideo(video) },
                onDelete = { viewModel.deleteVideo(video.id) },
                onToggleFavorite = { viewModel.toggleFavorite(video.id) }
            )
        }

        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun VideoListItem(
    video: VideoEntity,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isCurrent) Color(0x22FFFFFF) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("video_item_${video.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail 16:9 Card
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        colors = if (isCurrent) listOf(Color(0xFF8B0000), Color(0xFF1E1E28))
                        else listOf(Color(0xFF2A2A35), Color(0xFF161620))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCurrent) Icons.Default.Equalizer else Icons.Default.Movie,
                contentDescription = null,
                tint = if (isCurrent) YouTubeRed else Color(0xFF888899),
                modifier = Modifier.size(28.dp)
            )

            // Duration Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = TimeUtils.formatDuration(video.durationMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = video.title,
                color = if (isCurrent) YouTubeRed else Color.White,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = video.resolution,
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp
                )
                if (video.subtitleUri != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "CC",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (video.isFavorite) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = YouTubeRed,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Popup menu
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.background(YouTubeDarkCard)
            ) {
                DropdownMenuItem(
                    text = { Text(if (video.isFavorite) "Remove Favorite" else "Add Favorite", color = Color.White) },
                    onClick = {
                        menuExpanded = false
                        onToggleFavorite()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete from Queue", color = YouTubeRed) },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
