package com.turbodownloader.data.repository

import com.turbodownloader.data.database.DownloadDao
import com.turbodownloader.data.model.DownloadItem
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.model.FileCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val downloadDao: DownloadDao
) {

    fun getAllDownloads(): Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

    fun getActiveDownloads(): Flow<List<DownloadItem>> =
        downloadDao.getDownloadsByStatus(
            listOf(DownloadStatus.DOWNLOADING, DownloadStatus.PENDING, DownloadStatus.QUEUED)
        )

    fun getCompletedDownloads(): Flow<List<DownloadItem>> =
        downloadDao.getDownloadsByStatus(DownloadStatus.COMPLETED)

    fun getDownloadsByCategory(category: FileCategory): Flow<List<DownloadItem>> =
        downloadDao.getDownloadsByCategory(category)

    fun observeDownload(id: Long): Flow<DownloadItem?> =
        downloadDao.observeDownloadById(id)

    fun searchDownloads(query: String): Flow<List<DownloadItem>> =
        downloadDao.searchDownloads(query)

    suspend fun getDownloadById(id: Long): DownloadItem? =
        downloadDao.getDownloadById(id)

    suspend fun insertDownload(item: DownloadItem): Long =
        downloadDao.insertDownload(item)

    suspend fun updateDownload(item: DownloadItem) =
        downloadDao.updateDownload(item)

    suspend fun deleteDownload(item: DownloadItem) =
        downloadDao.deleteDownload(item)

    suspend fun deleteDownloadById(id: Long) =
        downloadDao.deleteDownloadById(id)

    suspend fun updateStatus(id: Long, status: DownloadStatus) =
        downloadDao.updateStatus(id, status)

    suspend fun updateProgress(id: Long, downloadedSize: Long, speed: Long) =
        downloadDao.updateProgress(id, downloadedSize, speed)

    suspend fun markCompleted(id: Long) =
        downloadDao.markCompleted(id)

    suspend fun markFailed(id: Long, error: String?) =
        downloadDao.markFailed(id, error = error)

    suspend fun getActiveDownloadsList(): List<DownloadItem> =
        downloadDao.getActiveDownloads()

    suspend fun getCountByStatus(status: DownloadStatus): Int =
        downloadDao.getCountByStatus(status)

    suspend fun updateFileInfo(id: Long, fileSize: Long, resumable: Boolean) =
        downloadDao.updateFileInfo(id, fileSize, resumable)

    suspend fun deleteAllCompleted() =
        downloadDao.deleteAllByStatus(DownloadStatus.COMPLETED)
}
