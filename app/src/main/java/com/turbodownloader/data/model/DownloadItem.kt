package com.turbodownloader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String = "",
    val fileSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val speed: Long = 0L,
    val threadCount: Int = 4,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val category: FileCategory = FileCategory.OTHER,
    val resumable: Boolean = true,
    val headers: String = "{}"
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
    QUEUED
}

enum class FileCategory {
    VIDEO,
    AUDIO,
    IMAGE,
    DOCUMENT,
    ARCHIVE,
    APK,
    OTHER;

    companion object {
        fun fromMimeType(mimeType: String): FileCategory {
            return when {
                mimeType.startsWith("video/") -> VIDEO
                mimeType.startsWith("audio/") -> AUDIO
                mimeType.startsWith("image/") -> IMAGE
                mimeType.contains("pdf") || mimeType.contains("document") ||
                    mimeType.contains("text/") || mimeType.contains("spreadsheet") ||
                    mimeType.contains("presentation") -> DOCUMENT
                mimeType.contains("zip") || mimeType.contains("rar") ||
                    mimeType.contains("7z") || mimeType.contains("tar") ||
                    mimeType.contains("gz") || mimeType.contains("archive") -> ARCHIVE
                mimeType.contains("android") || mimeType.contains("apk") -> APK
                else -> OTHER
            }
        }

        fun fromFileName(fileName: String): FileCategory {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp" -> VIDEO
                "mp3", "aac", "flac", "ogg", "wav", "wma", "m4a" -> AUDIO
                "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "ico" -> IMAGE
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt",
                "csv", "rtf", "odt", "ods" -> DOCUMENT
                "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> ARCHIVE
                "apk", "xapk" -> APK
                else -> OTHER
            }
        }
    }
}
