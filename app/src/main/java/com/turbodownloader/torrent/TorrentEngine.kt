package com.turbodownloader.torrent

import android.content.Context
import com.turbodownloader.data.model.DownloadItem
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.model.FileCategory
import com.turbodownloader.data.repository.DownloadRepository
import com.turbodownloader.download.DownloadEngine
import com.turbodownloader.util.FileUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class TorrentInfo(
    val name: String,
    val totalSize: Long,
    val files: List<TorrentFile>,
    val infoHash: String,
    val trackers: List<String>
)

data class TorrentFile(
    val path: String,
    val size: Long
)

@Singleton
class TorrentEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val client = OkHttpClient.Builder().build()

    companion object {
        private val MAGNET_PATTERN = Regex("magnet:\\?xt=urn:btih:([a-fA-F0-9]{40}|[a-zA-Z2-7]{32})")

        fun isMagnetLink(url: String): Boolean = url.startsWith("magnet:?")

        fun isTorrentFile(url: String): Boolean = url.lowercase().endsWith(".torrent")

        fun parseMagnetLink(magnet: String): TorrentInfo? {
            val match = MAGNET_PATTERN.find(magnet) ?: return null
            val infoHash = match.groupValues[1].let {
                if (it.length == 32) base32ToHex(it) else it.lowercase()
            }

            val name = Regex("[?&]dn=([^&]+)").find(magnet)?.groupValues?.get(1)?.let {
                URLDecoder.decode(it, "UTF-8")
            } ?: "torrent_$infoHash"

            val trackers = Regex("[?&]tr=([^&]+)").findAll(magnet).map {
                URLDecoder.decode(it.groupValues[1], "UTF-8")
            }.toList()

            return TorrentInfo(
                name = name,
                totalSize = 0L,
                files = emptyList(),
                infoHash = infoHash,
                trackers = trackers
            )
        }

        private fun base32ToHex(base32: String): String {
            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
            val input = base32.uppercase()
            var bits = ""
            for (c in input) {
                val idx = alphabet.indexOf(c)
                if (idx == -1) continue
                bits += Integer.toBinaryString(idx or 0x20).substring(1)
            }
            val bytes = ByteArray(bits.length / 8)
            for (i in bytes.indices) {
                bytes[i] = bits.substring(i * 8, i * 8 + 8).toInt(2).toByte()
            }
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    fun startTorrentDownload(torrentInfo: TorrentInfo, downloadId: Long) {
        val job = scope.launch {
            try {
                repository.updateStatus(downloadId, DownloadStatus.DOWNLOADING)

                val downloadDir = FileUtil.getDownloadDirectory(context)
                val torrentDir = File(downloadDir, torrentInfo.name)
                torrentDir.mkdirs()

                val cacheApiUrl = "https://itorrents.org/torrent/${torrentInfo.infoHash.uppercase()}.torrent"
                val request = Request.Builder().url(cacheApiUrl).build()

                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val torrentFile = File(torrentDir, "${torrentInfo.name}.torrent")
                        response.body?.byteStream()?.use { input ->
                            torrentFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                } catch (_: Exception) {
                }

                repository.markCompleted(downloadId)
            } catch (e: Exception) {
                repository.markFailed(downloadId, "Torrent error: ${e.message}")
            } finally {
                activeJobs.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    fun cancelTorrent(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        activeJobs.remove(downloadId)
        scope.launch {
            repository.updateStatus(downloadId, DownloadStatus.CANCELLED)
        }
    }
}
