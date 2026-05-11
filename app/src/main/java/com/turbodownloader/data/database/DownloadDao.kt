package com.turbodownloader.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.turbodownloader.data.model.DownloadItem
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.model.FileCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY createdAt DESC")
    fun getDownloadsByStatus(statuses: List<DownloadStatus>): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE category = :category ORDER BY createdAt DESC")
    fun getDownloadsByCategory(category: FileCategory): Flow<List<DownloadItem>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadItem?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeDownloadById(id: Long): Flow<DownloadItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(item: DownloadItem): Long

    @Update
    suspend fun updateDownload(item: DownloadItem)

    @Delete
    suspend fun deleteDownload(item: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus)

    @Query("UPDATE downloads SET downloadedSize = :downloadedSize, speed = :speed WHERE id = :id")
    suspend fun updateProgress(id: Long, downloadedSize: Long, speed: Long)

    @Query("UPDATE downloads SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, status: DownloadStatus = DownloadStatus.COMPLETED, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun markFailed(id: Long, status: DownloadStatus = DownloadStatus.FAILED, error: String?)

    @Query("SELECT COUNT(*) FROM downloads WHERE status = :status")
    suspend fun getCountByStatus(status: DownloadStatus): Int

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING', 'PENDING', 'QUEUED') ORDER BY createdAt ASC")
    suspend fun getActiveDownloads(): List<DownloadItem>

    @Query("SELECT * FROM downloads WHERE fileName LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchDownloads(query: String): Flow<List<DownloadItem>>

    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteAllByStatus(status: DownloadStatus)

    @Query("UPDATE downloads SET fileSize = :fileSize, resumable = :resumable WHERE id = :id")
    suspend fun updateFileInfo(id: Long, fileSize: Long, resumable: Boolean)
}
