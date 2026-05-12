package com.turbodownloader.torrent

import android.content.Context
import com.turbodownloader.data.model.DownloadStatus
import com.turbodownloader.data.repository.DownloadRepository
import com.turbodownloader.util.FileUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.alerts.Alert
import org.libtorrent4j.alerts.AlertType
import org.libtorrent4j.alerts.AddTorrentAlert
import org.libtorrent4j.alerts.TorrentFinishedAlert
import org.libtorrent4j.swig.torrent_flags_t
import java.io.File
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class TorrentInfo(
    val name: String,
    val totalSize: Long,
    val files: List<TorrentFile>,
    val infoHash: String,
    val trackers: List<String>,
    val magnetUri: String = ""
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
    private var sessionManager: SessionManager? = null
    private val torrentHandles = ConcurrentHashMap<Long, TorrentHandle>()
    private val finishedTorrents = ConcurrentHashMap<Long, Boolean>()

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
                trackers = trackers,
                magnetUri = magnet
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

    @Synchronized
    private fun getOrCreateSession(): SessionManager {
        sessionManager?.let {
            if (it.isRunning) return it
        }

        try {
            val sp = SettingsPack()
            sp.setEnableDht(true)
            sp.setEnableLsd(true)
            sp.activeDownloads(3)
            sp.activeSeeds(3)
            sp.connectionsLimit(200)
            sp.downloadRateLimit(0)
            sp.uploadRateLimit(0)

            val session = SessionManager(false)
            session.start(SessionParams(sp))

            // Wait for DHT to bootstrap
            var dhtWait = 0
            while (!session.isDhtRunning && dhtWait < 10) {
                Thread.sleep(1000)
                dhtWait++
            }

            sessionManager = session
            return session
        } catch (e: UnsatisfiedLinkError) {
            throw RuntimeException("Torrent library not available on this device: ${e.message}")
        }
    }

    fun startTorrentDownload(torrentInfo: TorrentInfo, downloadId: Long) {
        val job = scope.launch {
            try {
                repository.updateStatus(downloadId, DownloadStatus.DOWNLOADING)
                finishedTorrents[downloadId] = false

                val downloadDir = FileUtil.getDownloadDirectory(context)
                val saveDir = File(downloadDir, "Torrents")
                saveDir.mkdirs()

                val session = getOrCreateSession()

                session.addListener(object : AlertListener {
                    override fun types(): IntArray? = null

                    override fun alert(alert: Alert<*>) {
                        when (alert.type()) {
                            AlertType.ADD_TORRENT -> {
                                try {
                                    val addAlert = alert as AddTorrentAlert
                                    addAlert.handle().resume()
                                } catch (_: Exception) {}
                            }
                            AlertType.TORRENT_FINISHED -> {
                                try {
                                    val finAlert = alert as TorrentFinishedAlert
                                    val handle = finAlert.handle()
                                    torrentHandles.entries.find { it.value == handle }?.let { entry ->
                                        finishedTorrents[entry.key] = true
                                    }
                                } catch (_: Exception) {}
                            }
                            else -> {}
                        }
                    }
                })

                // Download magnet URI - this resolves metadata from peers and starts downloading
                session.download(torrentInfo.magnetUri, saveDir, torrent_flags_t())

                // Wait for metadata using find() with Sha1Hash
                var attempts = 0
                var handle: TorrentHandle? = null
                val sha1 = Sha1Hash.parseHex(torrentInfo.infoHash)

                while (attempts < 180 && isActive) {
                    try {
                        handle = session.find(sha1)
                        if (handle != null && handle.status().hasMetadata()) break
                        if (handle != null && !handle.status().hasMetadata()) {
                            handle = null
                        }
                    } catch (_: Exception) {
                        handle = null
                    }
                    delay(1000)
                    attempts++
                }

                if (handle == null) {
                    repository.markFailed(downloadId, "Could not find peers or fetch metadata after ${attempts}s. Check your internet connection and try again.")
                    return@launch
                }

                torrentHandles[downloadId] = handle

                val ti = handle.torrentFile()
                if (ti != null) {
                    repository.updateFileInfo(downloadId, ti.totalSize(), true)
                    val item = repository.getDownloadById(downloadId)
                    if (item != null) {
                        val filePath = File(saveDir, ti.name()).absolutePath
                        repository.updateDownload(item.copy(
                            filePath = filePath,
                            fileName = ti.name(),
                            fileSize = ti.totalSize()
                        ))
                    }
                }

                // Monitor progress until finished
                while (isActive) {
                    if (finishedTorrents[downloadId] == true) break

                    try {
                        val status = handle.status()
                        val downloaded = status.totalDone()
                        val speed = status.downloadRate().toLong()
                        repository.updateProgress(downloadId, downloaded, speed)

                        // Update seeder/leecher counts
                        val seeders = status.numSeeds()
                        val leechers = status.numPeers() - seeders
                        repository.updatePeerInfo(downloadId, seeders, if (leechers > 0) leechers else 0)
                    } catch (_: Exception) {}

                    delay(1000)
                }

                repository.updateProgress(downloadId, handle.status().totalDone(), 0)
                repository.markCompleted(downloadId)

                handle.pause()

            } catch (e: Exception) {
                repository.markFailed(downloadId, "Torrent error: ${e.message}")
            } finally {
                activeJobs.remove(downloadId)
                torrentHandles.remove(downloadId)
                finishedTorrents.remove(downloadId)
            }
        }
        activeJobs[downloadId] = job
    }

    fun cancelTorrent(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        torrentHandles[downloadId]?.let { handle ->
            try {
                sessionManager?.remove(handle)
            } catch (_: Exception) {}
        }
        activeJobs.remove(downloadId)
        torrentHandles.remove(downloadId)
        finishedTorrents.remove(downloadId)
        scope.launch {
            repository.updateStatus(downloadId, DownloadStatus.CANCELLED)
        }
    }

    fun shutdown() {
        try {
            sessionManager?.stop()
            sessionManager = null
        } catch (_: Exception) {}
    }
}
