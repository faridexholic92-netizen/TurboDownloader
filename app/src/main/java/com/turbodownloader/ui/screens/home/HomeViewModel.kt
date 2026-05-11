package com.turbodownloader.ui.screens.home

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.turbodownloader.data.model.DownloadItem
import com.turbodownloader.data.model.DownloadRequest
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.model.FileCategory
import com.turbodownloader.data.repository.DownloadRepository
import com.turbodownloader.download.DownloadEngine
import com.turbodownloader.service.DownloadService
import com.turbodownloader.util.FileUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val repository: DownloadRepository,
    private val downloadEngine: DownloadEngine
) : AndroidViewModel(application) {

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val allDownloads = repository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads = repository.getActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads = repository.getCompletedDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloadCount = downloadEngine.activeDownloadCount

    fun selectCategory(category: FileCategory?) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addDownload(request: DownloadRequest) {
        viewModelScope.launch {
            val fileName = request.fileName.ifBlank {
                FileUtil.getFileNameFromUrl(request.url)
            }
            val mimeType = FileUtil.getMimeType(fileName)
            val category = FileCategory.fromFileName(fileName)
            val downloadDir = FileUtil.getDownloadDirectory(application)
            val uniqueName = FileUtil.getUniqueFileName(downloadDir, fileName)
            val filePath = File(downloadDir, uniqueName).absolutePath

            val item = DownloadItem(
                url = request.url,
                fileName = uniqueName,
                filePath = filePath,
                mimeType = mimeType,
                category = category,
                threadCount = request.threadCount
            )

            val id = repository.insertDownload(item)
            DownloadService.startDownload(application, id)
        }
    }

    fun pauseDownload(downloadId: Long) {
        DownloadService.pauseDownload(application, downloadId)
    }

    fun resumeDownload(downloadId: Long) {
        DownloadService.resumeDownload(application, downloadId)
    }

    fun cancelDownload(downloadId: Long) {
        DownloadService.cancelDownload(application, downloadId)
    }

    fun retryDownload(item: DownloadItem) {
        viewModelScope.launch {
            repository.updateStatus(item.id, DownloadStatus.PENDING)
            DownloadService.startDownload(application, item.id)
        }
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            repository.deleteDownloadById(downloadId)
        }
    }

    fun getDownloadProgress(downloadId: Long) = downloadEngine.getProgress(downloadId)

    fun getStorageInfo(): Pair<Long, Long> {
        val stat = Environment.getExternalStorageDirectory()
        val free = stat.freeSpace
        val total = stat.totalSpace
        return Pair(free, total)
    }
}
