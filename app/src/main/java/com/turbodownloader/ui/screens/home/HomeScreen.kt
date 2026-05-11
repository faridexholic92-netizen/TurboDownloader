package com.turbodownloader.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.turbodownloader.data.model.FileCategory
import com.turbodownloader.ui.components.AddDownloadDialog
import com.turbodownloader.ui.components.CategoryChip
import com.turbodownloader.ui.components.DownloadItemCard
import com.turbodownloader.util.FileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val allDownloads by viewModel.allDownloads.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val completedDownloads by viewModel.completedDownloads.collectAsState()
    val activeCount by viewModel.activeDownloadCount.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val filteredDownloads = if (selectedCategory != null) {
        allDownloads.filter { it.category == selectedCategory }
    } else {
        allDownloads
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Turbo Downloader",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${activeCount} active · ${completedDownloads.size} completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, "Add download") },
                text = { Text("New Download") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Stats cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Downloading,
                        label = "Active",
                        value = "$activeCount",
                        modifier = Modifier.weight(1f),
                        gradient = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                    StatCard(
                        icon = Icons.Default.DownloadDone,
                        label = "Completed",
                        value = "${completedDownloads.size}",
                        modifier = Modifier.weight(1f),
                        gradient = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                        )
                    )
                    val (free, total) = remember { viewModel.getStorageInfo() }
                    StatCard(
                        icon = Icons.Default.Storage,
                        label = "Free",
                        value = FileUtil.formatFileSize(free),
                        modifier = Modifier.weight(1f),
                        gradient = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                        )
                    )
                }
            }

            // Category filter
            item {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(FileCategory.entries) { category ->
                        CategoryChip(
                            category = category,
                            selected = selectedCategory == category,
                            onClick = {
                                viewModel.selectCategory(
                                    if (selectedCategory == category) null else category
                                )
                            }
                        )
                    }
                }
            }

            // Section header
            if (filteredDownloads.isNotEmpty()) {
                item {
                    Text(
                        text = if (selectedCategory != null) "${selectedCategory!!.name} Downloads" else "All Downloads",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Download items
            items(
                items = filteredDownloads,
                key = { it.id }
            ) { item ->
                val progress = viewModel.getDownloadProgress(item.id)
                DownloadItemCard(
                    item = item,
                    onPause = { viewModel.pauseDownload(item.id) },
                    onResume = { viewModel.resumeDownload(item.id) },
                    onCancel = { viewModel.cancelDownload(item.id) },
                    onRetry = { viewModel.retryDownload(item) },
                    onOpen = { /* Open file */ },
                    speed = progress?.speed ?: item.speed,
                    downloadedSize = progress?.downloadedBytes ?: item.downloadedSize
                )
            }

            // Empty state
            if (filteredDownloads.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No downloads yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "Tap + to start downloading",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDownloadDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { request ->
                viewModel.addDownload(request)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    gradient: List<androidx.compose.ui.graphics.Color> = emptyList()
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (gradient.isNotEmpty()) {
                        Modifier.background(Brush.verticalGradient(gradient))
                    } else Modifier
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
