package com.turbodownloader.download

import android.content.Context
import com.turbodownloader.data.model.DownloadItem
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.repository.DownloadRepository
import com.turbodownloader.util.FileUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val downloadProgress = ConcurrentHashMap<Long, DownloadProgress>()

    private val _activeDownloadCount = MutableStateFlow(0)
    val activeDownloadCount: StateFlow<Int> = _activeDownloadCount

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    var maxConcurrentDownloads = 3

    data class DownloadProgress(
        val downloadId: Long,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: Long,
        val status: DownloadStatus
    ) {
        val progress: Float
            get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
    }

    fun getProgress(downloadId: Long): DownloadProgress? = downloadProgress[downloadId]

    fun startDownload(downloadItem: DownloadItem) {
        if (activeJobs.containsKey(downloadItem.id)) return

        val job = scope.launch {
            try {
                repository.updateStatus(downloadItem.id, DownloadStatus.DOWNLOADING)
                _activeDownloadCount.value = activeJobs.size + 1

                val fileInfo = fetchFileInfo(downloadItem.url)
                val totalSize = fileInfo.contentLength
                val supportsRange = fileInfo.acceptsRanges

                repository.updateFileInfo(downloadItem.id, totalSize, supportsRange)

                val downloadDir = FileUtil.getDownloadDirectory(context)
                val filePath = File(downloadDir, downloadItem.fileName).absolutePath

                if (supportsRange && totalSize > 0 && downloadItem.threadCount > 1) {
                    multiThreadDownload(downloadItem.id, downloadItem.url, filePath, totalSize, downloadItem.threadCount)
                } else {
                    singleThreadDownload(downloadItem.id, downloadItem.url, filePath)
                }

                repository.markCompleted(downloadItem.id)
                downloadProgress[downloadItem.id] = DownloadProgress(
                    downloadItem.id, totalSize, totalSize, 0, DownloadStatus.COMPLETED
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                repository.markFailed(downloadItem.id, e.message)
                downloadProgress[downloadItem.id] = DownloadProgress(
                    downloadItem.id, 0, 0, 0, DownloadStatus.FAILED
                )
            } finally {
                activeJobs.remove(downloadItem.id)
                _activeDownloadCount.value = activeJobs.size
            }
        }

        activeJobs[downloadItem.id] = job
    }

    fun pauseDownload(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        _activeDownloadCount.value = activeJobs.size
        scope.launch {
            repository.updateStatus(downloadId, DownloadStatus.PAUSED)
            downloadProgress[downloadId]?.let { progress ->
                downloadProgress[downloadId] = progress.copy(status = DownloadStatus.PAUSED)
            }
        }
    }

    fun resumeDownload(downloadItem: DownloadItem) {
        startDownload(downloadItem)
    }

    fun cancelDownload(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        downloadProgress.remove(downloadId)
        _activeDownloadCount.value = activeJobs.size
        scope.launch {
            repository.updateStatus(downloadId, DownloadStatus.CANCELLED)
        }
    }

    fun isDownloading(downloadId: Long): Boolean = activeJobs.containsKey(downloadId)

    private data class FileInfo(
        val contentLength: Long,
        val acceptsRanges: Boolean,
        val mimeType: String
    )

    private suspend fun fetchFileInfo(url: String): FileInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .head()
            .build()

        client.newCall(request).execute().use { response ->
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1
            val acceptRanges = response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true
            val mimeType = response.header("Content-Type") ?: ""
            FileInfo(contentLength, acceptRanges, mimeType)
        }
    }

    private suspend fun singleThreadDownload(
        downloadId: Long,
        url: String,
        filePath: String
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        var lastUpdateTime = System.currentTimeMillis()
        var lastDownloadedBytes = 0L

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Server error: ${response.code}")

            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            val file = File(filePath)
            file.parentFile?.mkdirs()

            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var downloadedBytes = 0L
                val inputStream = body.byteStream()

                while (true) {
                    if (!isActive) break
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    output.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime >= 500) {
                        val elapsed = (now - lastUpdateTime) / 1000.0
                        val speed = ((downloadedBytes - lastDownloadedBytes) / elapsed).toLong()

                        downloadProgress[downloadId] = DownloadProgress(
                            downloadId, downloadedBytes, totalBytes, speed, DownloadStatus.DOWNLOADING
                        )
                        repository.updateProgress(downloadId, downloadedBytes, speed)

                        lastUpdateTime = now
                        lastDownloadedBytes = downloadedBytes
                    }
                }
            }
        }
    }

    private suspend fun multiThreadDownload(
        downloadId: Long,
        url: String,
        filePath: String,
        totalSize: Long,
        threadCount: Int
    ) = withContext(Dispatchers.IO) {
        val file = File(filePath)
        file.parentFile?.mkdirs()

        val raf = RandomAccessFile(file, "rw")
        raf.setLength(totalSize)
        raf.close()

        val chunkSize = totalSize / threadCount
        val threadProgress = LongArray(threadCount)
        var lastUpdateTime = System.currentTimeMillis()
        var lastTotalDownloaded = 0L

        val jobs = (0 until threadCount).map { threadIndex ->
            async {
                val startByte = threadIndex * chunkSize
                val endByte = if (threadIndex == threadCount - 1) totalSize - 1 else (startByte + chunkSize - 1)

                val request = Request.Builder()
                    .url(url)
                    .header("Range", "bytes=$startByte-$endByte")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 206) {
                        throw Exception("Server error: ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Empty response body")
                    val partFile = RandomAccessFile(file, "rw")
                    partFile.seek(startByte)

                    val buffer = ByteArray(8192)
                    val inputStream = body.byteStream()
                    var bytesReadForThread = 0L

                    while (isActive) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break

                        partFile.write(buffer, 0, bytesRead)
                        bytesReadForThread += bytesRead
                        threadProgress[threadIndex] = bytesReadForThread

                        val now = System.currentTimeMillis()
                        if (now - lastUpdateTime >= 500) {
                            val totalDownloaded = threadProgress.sum()
                            val elapsed = (now - lastUpdateTime) / 1000.0
                            val speed = if (elapsed > 0) ((totalDownloaded - lastTotalDownloaded) / elapsed).toLong() else 0L

                            downloadProgress[downloadId] = DownloadProgress(
                                downloadId, totalDownloaded, totalSize, speed, DownloadStatus.DOWNLOADING
                            )

                            scope.launch {
                                repository.updateProgress(downloadId, totalDownloaded, speed)
                            }
                            lastTotalDownloaded = totalDownloaded
                            lastUpdateTime = now
                        }
                    }

                    partFile.close()
                }
            }
        }

        jobs.awaitAll()
    }

    fun cancelAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        _activeDownloadCount.value = 0
    }
}
