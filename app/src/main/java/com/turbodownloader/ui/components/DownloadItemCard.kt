package com.turbodownloader.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.turbodownloader.data.model.DownloadItem
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.model.FileCategory
import com.turbodownloader.ui.theme.*
import com.turbodownloader.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadItemCard(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
    speed: Long = 0L,
    downloadedSize: Long = item.downloadedSize,
    eta: Long = -1L
) {
    val progress by animateFloatAsState(
        targetValue = if (item.fileSize > 0) downloadedSize.toFloat() / item.fileSize else 0f,
        animationSpec = tween(300),
        label = "progress"
    )

    val statusColor by animateColorAsState(
        targetValue = when (item.status) {
            DownloadStatus.DOWNLOADING -> DownloadingColor
            DownloadStatus.PAUSED -> PausedColor
            DownloadStatus.COMPLETED -> CompletedColor
            DownloadStatus.FAILED, DownloadStatus.CANCELLED -> FailedColor
            else -> QueuedColor
        },
        label = "statusColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { if (item.status == DownloadStatus.COMPLETED) onOpen() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getCategoryColor(item.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(item.category),
                    contentDescription = null,
                    tint = getCategoryColor(item.category),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor)
                    )
                    Text(
                        text = getStatusText(item.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                    if (item.fileSize > 0) {
                        Text(
                            text = "${FileUtil.formatFileSize(downloadedSize)} / ${FileUtil.formatFileSize(item.fileSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(
                    visible = item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED,
                    enter = expandVertically() + fadeIn()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                        if (item.status == DownloadStatus.DOWNLOADING && speed > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = FileUtil.formatSpeed(speed),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (eta > 0) {
                                    Text(
                                        text = "ETA: ${FileUtil.formatDuration(eta)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                if (item.fileSize > 0) {
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (item.status) {
                DownloadStatus.DOWNLOADING -> {
                    IconButton(onClick = onPause, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Pause, "Pause", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, "Cancel", tint = MaterialTheme.colorScheme.error)
                    }
                }
                DownloadStatus.PAUSED -> {
                    IconButton(onClick = onResume, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.PlayArrow, "Resume", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onCancel, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, "Cancel", tint = MaterialTheme.colorScheme.error)
                    }
                }
                DownloadStatus.COMPLETED -> {
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                    IconButton(onClick = onRetry, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Refresh, "Retry", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                else -> {}
            }
        }
    }
}

fun getCategoryIcon(category: FileCategory): ImageVector = when (category) {
    FileCategory.VIDEO -> Icons.Default.VideoFile
    FileCategory.AUDIO -> Icons.Default.MusicNote
    FileCategory.IMAGE -> Icons.Default.Image
    FileCategory.DOCUMENT -> Icons.Default.Description
    FileCategory.ARCHIVE -> Icons.Default.FolderZip
    FileCategory.APK -> Icons.Default.PhoneAndroid
    FileCategory.OTHER -> Icons.Default.InsertDriveFile
}

fun getCategoryColor(category: FileCategory): androidx.compose.ui.graphics.Color = when (category) {
    FileCategory.VIDEO -> VideoColor
    FileCategory.AUDIO -> AudioColor
    FileCategory.IMAGE -> ImageColor
    FileCategory.DOCUMENT -> DocumentColor
    FileCategory.ARCHIVE -> ArchiveColor
    FileCategory.APK -> ApkColor
    FileCategory.OTHER -> OtherColor
}

fun getStatusText(status: DownloadStatus): String = when (status) {
    DownloadStatus.PENDING -> "Pending"
    DownloadStatus.DOWNLOADING -> "Downloading"
    DownloadStatus.PAUSED -> "Paused"
    DownloadStatus.COMPLETED -> "Completed"
    DownloadStatus.FAILED -> "Failed"
    DownloadStatus.CANCELLED -> "Cancelled"
    DownloadStatus.QUEUED -> "Queued"
}
