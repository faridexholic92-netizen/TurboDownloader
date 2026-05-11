package com.turbodownloader.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.turbodownloader.MainActivity
import com.turbodownloader.R
import com.turbodownloader.TurboDownloaderApp
import com.turbodownloader.data.model.DownloadItem
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.repository.DownloadRepository
import com.turbodownloader.download.DownloadEngine
import com.turbodownloader.util.FileUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadEngine: DownloadEngine

    @Inject
    lateinit var repository: DownloadRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var progressJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    serviceScope.launch {
                        val item = repository.getDownloadById(downloadId) ?: return@launch
                        downloadEngine.startDownload(item)
                    }
                }
                startForeground(NOTIFICATION_ID, createNotification("Downloading..."))
                startProgressUpdates()
            }
            ACTION_PAUSE_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) downloadEngine.pauseDownload(downloadId)
            }
            ACTION_RESUME_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    serviceScope.launch {
                        val item = repository.getDownloadById(downloadId) ?: return@launch
                        downloadEngine.resumeDownload(item)
                    }
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) downloadEngine.cancelDownload(downloadId)
            }
            ACTION_STOP_SERVICE -> {
                downloadEngine.cancelAll()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                val activeCount = downloadEngine.activeDownloadCount.value
                if (activeCount == 0) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                updateNotification(activeCount)
                delay(1000)
            }
        }
    }

    private fun updateNotification(activeCount: Int) {
        val notification = createNotification("$activeCount download(s) in progress")
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TurboDownloaderApp.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("Turbo Downloader")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_DOWNLOAD = "com.turbodownloader.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.turbodownloader.PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.turbodownloader.RESUME_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.turbodownloader.CANCEL_DOWNLOAD"
        const val ACTION_STOP_SERVICE = "com.turbodownloader.STOP_SERVICE"
        const val EXTRA_DOWNLOAD_ID = "download_id"

        fun startDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startForegroundService(intent)
        }

        fun pauseDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }

        fun resumeDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RESUME_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startForegroundService(intent)
        }

        fun cancelDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }
}
