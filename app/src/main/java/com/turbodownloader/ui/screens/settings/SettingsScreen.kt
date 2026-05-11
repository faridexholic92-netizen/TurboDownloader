package com.turbodownloader.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var maxConcurrentDownloads by remember { mutableFloatStateOf(3f) }
    var maxThreadsPerDownload by remember { mutableFloatStateOf(4f) }
    var wifiOnly by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    var autoRetry by remember { mutableStateOf(true) }
    var speedLimit by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text("Settings", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Download Settings
            SettingsSection(title = "Download") {
                SettingsSliderItem(
                    icon = Icons.Default.Tune,
                    title = "Concurrent Downloads",
                    subtitle = "${maxConcurrentDownloads.toInt()} downloads at once",
                    value = maxConcurrentDownloads,
                    onValueChange = { maxConcurrentDownloads = it },
                    valueRange = 1f..5f,
                    steps = 3
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSliderItem(
                    icon = Icons.Default.Speed,
                    title = "Threads per Download",
                    subtitle = "${maxThreadsPerDownload.toInt()} threads",
                    value = maxThreadsPerDownload,
                    onValueChange = { maxThreadsPerDownload = it },
                    valueRange = 1f..8f,
                    steps = 6
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    icon = Icons.Default.Wifi,
                    title = "Wi-Fi Only",
                    subtitle = "Download only when connected to Wi-Fi",
                    checked = wifiOnly,
                    onCheckedChange = { wifiOnly = it }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    icon = Icons.Default.NetworkCheck,
                    title = "Speed Limit",
                    subtitle = "Limit download speed to save bandwidth",
                    checked = speedLimit,
                    onCheckedChange = { speedLimit = it }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Auto Retry",
                    subtitle = "Automatically retry failed downloads",
                    checked = autoRetry,
                    onCheckedChange = { autoRetry = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Appearance
            SettingsSection(title = "Appearance") {
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Use dark theme",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Show download notifications",
                    checked = notifications,
                    onCheckedChange = { notifications = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage
            SettingsSection(title = "Storage") {
                SettingsClickItem(
                    icon = Icons.Default.FolderOpen,
                    title = "Download Location",
                    subtitle = "Download/TurboDownloader",
                    onClick = { }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsClickItem(
                    icon = Icons.Default.Delete,
                    title = "Clear Download History",
                    subtitle = "Remove completed download records",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About
            SettingsSection(title = "About") {
                SettingsClickItem(
                    icon = Icons.Default.Info,
                    title = "Turbo Downloader",
                    subtitle = "Version 1.0.0",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSliderItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${valueRange.start.toInt()}", style = MaterialTheme.typography.labelSmall)
            Text("${valueRange.endInclusive.toInt()}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SettingsClickItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
