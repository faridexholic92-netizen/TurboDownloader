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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.turbodownloader.ui.theme.ThemeManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(themeManager: ThemeManager) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val maxConcurrentDownloads by themeManager.concurrentDownloads.collectAsState(initial = 3f)
    val maxThreadsPerDownload by themeManager.threadsPerDownload.collectAsState(initial = 4f)
    val wifiOnly by themeManager.isWifiOnly.collectAsState(initial = false)
    val darkMode by themeManager.isDarkMode.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    val notifications by themeManager.isNotifications.collectAsState(initial = true)
    val autoRetry by themeManager.isAutoRetry.collectAsState(initial = true)
    val speedLimit by themeManager.isSpeedLimit.collectAsState(initial = false)
    val downloadPath by themeManager.downloadPath.collectAsState(initial = "")
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val folderPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val contentResolver = context.contentResolver
            contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val docUri = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, it)
            val path = docUri?.uri?.toString() ?: it.toString()
            coroutineScope.launch { themeManager.setDownloadPath(path) }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            SettingsSection("Download") {
                SettingsSliderItem(Icons.Default.Tune, "Concurrent Downloads", "${maxConcurrentDownloads.toInt()} downloads at once", maxConcurrentDownloads, { coroutineScope.launch { themeManager.setConcurrentDownloads(it) } }, 1f..5f, 3)
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSliderItem(Icons.Default.Speed, "Threads per Download", "${maxThreadsPerDownload.toInt()} threads", maxThreadsPerDownload, { coroutineScope.launch { themeManager.setThreadsPerDownload(it) } }, 1f..8f, 6)
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSwitchItem(Icons.Default.Wifi, "Wi-Fi Only", "Download only when connected to Wi-Fi", wifiOnly) { coroutineScope.launch { themeManager.setWifiOnly(it) } }
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSwitchItem(Icons.Default.NetworkCheck, "Speed Limit", "Limit download speed to save bandwidth", speedLimit) { coroutineScope.launch { themeManager.setSpeedLimit(it) } }
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSwitchItem(Icons.Default.BatteryChargingFull, "Auto Retry", "Automatically retry failed downloads", autoRetry) { coroutineScope.launch { themeManager.setAutoRetry(it) } }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection("Appearance") {
                SettingsSwitchItem(Icons.Default.DarkMode, "Dark Mode AMOLED", "Pure black AMOLED theme", darkMode) { coroutineScope.launch { themeManager.setDarkMode(it) } }
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsSwitchItem(Icons.Default.Notifications, "Notifications", "Show download notifications", notifications) { coroutineScope.launch { themeManager.setNotifications(it) } }
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection("Storage") {
                val displayPath = if (downloadPath.isBlank()) "Download/TurboDownloader" else {
                    val decoded = android.net.Uri.decode(downloadPath)
                    val lastSegment = decoded.substringAfterLast(":")
                    if (lastSegment.isNotBlank()) lastSegment else decoded.substringAfterLast("/")
                }
                SettingsClickItem(Icons.Default.FolderOpen, "Download Location", displayPath) { folderPicker.launch(null) }
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsClickItem(Icons.Default.Delete, "Clear Download History", "Remove completed download records") {}
            }

            Spacer(Modifier.height(16.dp))

            SettingsSection("About") {
                SettingsClickItem(Icons.Default.Info, "Turbo Downloader", "Version 1.0.0") { showAboutDialog = true }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "Turbo Downloader",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "⚡ Fast multi-threaded download manager",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Made by Naga \uD83C\uDDF2\uD83C\uDDFE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) { content() }
    }
}

@Composable
fun SettingsSwitchItem(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSliderItem(icon: ImageVector, title: String, subtitle: String, value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, steps: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${valueRange.start.toInt()}", style = MaterialTheme.typography.labelSmall)
            Text("${valueRange.endInclusive.toInt()}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SettingsClickItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
