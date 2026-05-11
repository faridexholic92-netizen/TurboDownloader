package com.turbodownloader.util

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.File
import java.net.URLDecoder
import java.text.DecimalFormat

object FileUtil {

    fun getDownloadDirectory(context: Context, customPath: String? = null): File {
        val dir = if (!customPath.isNullOrBlank()) {
            File(customPath)
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "TurboDownloader"
            )
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getFileNameFromUrl(url: String): String {
        return try {
            val decoded = URLDecoder.decode(url, "UTF-8")
            val path = decoded.substringBefore("?").substringBefore("#")
            val name = path.substringAfterLast("/")
            if (name.isNotBlank() && name.contains(".")) name else "download_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.##")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatFileSize(bytesPerSecond)}/s"
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format("%d:%02d", minutes, secs)
            else -> String.format("0:%02d", secs)
        }
    }

    fun getUniqueFileName(directory: File, fileName: String): String {
        var file = File(directory, fileName)
        if (!file.exists()) return fileName

        val name = fileName.substringBeforeLast('.')
        val ext = fileName.substringAfterLast('.', "")
        var counter = 1

        while (file.exists()) {
            val newName = if (ext.isNotEmpty()) "${name}_($counter).$ext" else "${name}_($counter)"
            file = File(directory, newName)
            counter++
        }
        return file.name
    }

    fun getEstimatedTimeRemaining(downloadedBytes: Long, totalBytes: Long, speedBytesPerSec: Long): String {
        if (speedBytesPerSec <= 0 || totalBytes <= 0) return "∞"
        val remainingBytes = totalBytes - downloadedBytes
        val remainingSeconds = remainingBytes / speedBytesPerSec
        return formatDuration(remainingSeconds)
    }
}
