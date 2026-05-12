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
import com.turbodownloader.download.YouTubeExtractor
import com.turbodownloader.download.YouTubeVideoInfo
import com.turbodownloader.service.DownloadService
import com.turbodownloader.torrent.TorrentEngine
import com.turbodownloader.ui.theme.ThemeManager
import com.turbodownloader.util.FileUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val application: Application,
    private val repository: DownloadRepository,
    private val downloadEngine: DownloadEngine,
    private val youTubeExtractor: YouTubeExtractor,
    private val themeManager: ThemeManager,
    private val torrentEngine: TorrentEngine
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
            val fileName = request.fileName.ifBlank { FileUtil.getFileNameFromUrl(request.url) }
            val mimeType = FileUtil.getMimeType(fileName)
            val category = FileCategory.fromFileName(fileName)
            val customPath = themeManager.downloadPath.first()
            val downloadDir = FileUtil.getDownloadDirectory(application, customPath.ifBlank { null })
            val uniqueName = FileUtil.getUniqueFileName(downloadDir, fileName)
            val filePath = File(downloadDir, uniqueName).absolutePath
            val threadCount = if (request.threadCount > 0) request.threadCount else themeManager.threadsPerDownload.first().toInt()

            val item = DownloadItem(
                url = request.url, fileName = uniqueName, filePath = filePath,
                mimeType = mimeType, category = category, threadCount = threadCount
            )

            val id = repository.insertDownload(item)
            DownloadService.startDownload(application, id)
        }
    }

    fun pauseDownload(downloadId: Long) { DownloadService.pauseDownload(application, downloadId) }
    fun resumeDownload(downloadId: Long) { DownloadService.resumeDownload(application, downloadId) }
    fun cancelDownload(downloadId: Long) { DownloadService.cancelDownload(application, downloadId) }

    fun retryDownload(item: DownloadItem) {
        viewModelScope.launch {
            repository.updateStatus(item.id, DownloadStatus.PENDING)
            DownloadService.startDownload(application, item.id)
        }
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            val item = repository.getDownloadById(downloadId)
            item?.let {
                val file = File(it.filePath)
                if (file.exists()) file.delete()
            }
            repository.deleteDownloadById(downloadId)
        }
    }

    private val _ytStreams = MutableStateFlow<List<YouTubeVideoInfo>>(emptyList())
    val ytStreams: StateFlow<List<YouTubeVideoInfo>> = _ytStreams

    private val _ytLoading = MutableStateFlow(false)
    val ytLoading: StateFlow<Boolean> = _ytLoading

    private val _ytError = MutableStateFlow<String?>(null)
    val ytError: StateFlow<String?> = _ytError

    private val _ytTitle = MutableStateFlow("")
    val ytTitle: StateFlow<String> = _ytTitle

    private val _showYtDialog = MutableStateFlow(false)
    val showYtDialog: StateFlow<Boolean> = _showYtDialog

    fun isYouTubeUrl(url: String): Boolean = YouTubeExtractor.isYouTubeUrl(url)

    fun extractYouTube(url: String) {
        _showYtDialog.value = true
        _ytLoading.value = true
        _ytError.value = null
        _ytStreams.value = emptyList()
        viewModelScope.launch {
            youTubeExtractor.extractVideoInfo(url).fold(
                onSuccess = { streams ->
                    _ytStreams.value = streams
                    _ytTitle.value = streams.firstOrNull()?.title ?: ""
                    _ytLoading.value = false
                },
                onFailure = { e ->
                    _ytError.value = e.message ?: "Failed to extract video info"
                    _ytLoading.value = false
                }
            )
        }
    }

    fun downloadYouTubeStream(stream: YouTubeVideoInfo) {
        _showYtDialog.value = false
        val fileName = "${stream.title}_${stream.quality}.${stream.fileExtension}"
        addDownload(DownloadRequest(url = stream.directUrl, fileName = fileName, threadCount = 1))
    }

    fun dismissYtDialog() {
        _showYtDialog.value = false
        _ytStreams.value = emptyList()
        _ytError.value = null
    }

    fun getDownloadProgress(downloadId: Long) = downloadEngine.getProgress(downloadId)

    fun addMagnetDownload(magnetUrl: String) {
        viewModelScope.launch {
            val torrentInfo = TorrentEngine.parseMagnetLink(magnetUrl) ?: run {
                _ytError.value = "Invalid magnet link"
                return@launch
            }
            val customPath = themeManager.downloadPath.first()
            val downloadDir = FileUtil.getDownloadDirectory(application, customPath.ifBlank { null })
            val filePath = File(downloadDir, torrentInfo.name).absolutePath

            val item = DownloadItem(
                url = magnetUrl,
                fileName = torrentInfo.name,
                filePath = filePath,
                mimeType = "application/x-bittorrent",
                category = FileCategory.OTHER,
                threadCount = 1
            )

            val id = repository.insertDownload(item)
            torrentEngine.startTorrentDownload(torrentInfo, id)
        }
    }

    fun getStorageInfo(): Pair<Long, Long> {
        val stat = Environment.getExternalStorageDirectory()
        return Pair(stat.freeSpace, stat.totalSpace)
    }
}
