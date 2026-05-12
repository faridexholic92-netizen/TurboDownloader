package com.turbodownloader.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class YouTubeVideoInfo(
    val title: String,
    val directUrl: String,
    val mimeType: String,
    val quality: String,
    val fileExtension: String,
    val fileSize: Long = 0L
)

@Singleton
class YouTubeExtractor @Inject constructor(
    private val client: OkHttpClient
) {

    companion object {
        private val YOUTUBE_PATTERNS = listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([\\w-]{11})"),
            Pattern.compile("(?:https?://)?(?:m\\.)?youtube\\.com/watch\\?v=([\\w-]{11})"),
            Pattern.compile("(?:https?://)?youtu\\.be/([\\w-]{11})"),
            Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/shorts/([\\w-]{11})")
        )

        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.r4fo.com",
            "https://api.piped.projectsegfau.lt"
        )

        fun isYouTubeUrl(url: String): Boolean {
            return YOUTUBE_PATTERNS.any { it.matcher(url).find() }
        }

        fun extractVideoId(url: String): String? {
            for (pattern in YOUTUBE_PATTERNS) {
                val matcher = pattern.matcher(url)
                if (matcher.find()) return matcher.group(1)
            }
            return null
        }
    }

    suspend fun extractVideoInfo(youtubeUrl: String): Result<List<YouTubeVideoInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val videoId = extractVideoId(youtubeUrl)
                    ?: return@withContext Result.failure(Exception("Invalid YouTube URL"))

                for (instance in PIPED_INSTANCES) {
                    try {
                        val result = extractFromPiped(instance, videoId)
                        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
                            return@withContext result
                        }
                    } catch (_: Exception) {
                    }
                }

                extractFromHtml(videoId)
            } catch (e: Exception) {
                Result.failure(Exception("YouTube extraction failed: ${e.message}"))
            }
        }

    private fun extractFromPiped(apiBase: String, videoId: String): Result<List<YouTubeVideoInfo>> {
        val request = Request.Builder()
            .url("$apiBase/streams/$videoId")
            .header("User-Agent", "TurboDownloader/1.0")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return Result.failure(Exception("Piped API error: ${response.code}"))

        val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
        val json = JSONObject(body)

        if (json.has("error")) {
            return Result.failure(Exception(json.getString("error")))
        }

        val title = json.optString("title", "youtube_$videoId")
            .replace("[^a-zA-Z0-9\\s\\-_]".toRegex(), "")
            .replace("\\s+".toRegex(), "_")
            .take(100)

        val streams = mutableListOf<YouTubeVideoInfo>()

        val videoStreams = json.optJSONArray("videoStreams")
        if (videoStreams != null) {
            for (i in 0 until videoStreams.length()) {
                val stream = videoStreams.getJSONObject(i)
                val url = stream.optString("url", "")
                if (url.isEmpty()) continue
                val quality = stream.optString("quality", "unknown")
                val mimeType = stream.optString("mimeType", "video/mp4")
                val ext = if (mimeType.contains("webm")) "webm" else "mp4"
                val contentLength = stream.optLong("contentLength", 0L)
                val videoOnly = stream.optBoolean("videoOnly", false)
                val displayQuality = if (videoOnly) "$quality (video only)" else quality

                streams.add(YouTubeVideoInfo(title, url, mimeType.substringBefore(";"), displayQuality, ext, contentLength))
            }
        }

        val audioStreams = json.optJSONArray("audioStreams")
        if (audioStreams != null) {
            for (i in 0 until audioStreams.length()) {
                val stream = audioStreams.getJSONObject(i)
                val url = stream.optString("url", "")
                if (url.isEmpty()) continue
                val mimeType = stream.optString("mimeType", "audio/mp4")
                val bitrate = stream.optInt("bitrate", 0)
                val quality = stream.optString("quality", "${bitrate / 1000}kbps")
                val ext = when {
                    mimeType.contains("webm") || mimeType.contains("opus") -> "webm"
                    else -> "m4a"
                }
                val contentLength = stream.optLong("contentLength", 0L)

                streams.add(YouTubeVideoInfo(title, url, mimeType.substringBefore(";"), "Audio $quality", ext, contentLength))
            }
        }

        return if (streams.isNotEmpty()) Result.success(streams) else Result.failure(Exception("No streams found"))
    }

    private fun extractFromHtml(videoId: String): Result<List<YouTubeVideoInfo>> {
        val infoUrl = "https://www.youtube.com/watch?v=$videoId"
        val request = Request.Builder()
            .url(infoUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return Result.failure(Exception("HTTP ${response.code}"))

        val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
        val title = extractTitle(body) ?: "youtube_${videoId}"
        val streams = extractStreams(body, title)

        return if (streams.isEmpty()) {
            Result.failure(Exception("Could not extract streams. Video may be restricted."))
        } else {
            Result.success(streams)
        }
    }

    private fun extractTitle(html: String): String? {
        val idx = html.indexOf("<title>")
        if (idx == -1) return null
        val end = html.indexOf("</title>", idx)
        if (end == -1) return null
        return html.substring(idx + 7, end)
            .replace(" - YouTube", "").trim()
            .replace("[^a-zA-Z0-9\\s\\-_]".toRegex(), "")
            .replace("\\s+".toRegex(), "_")
            .take(100).ifBlank { null }
    }

    private fun extractStreams(html: String, title: String): List<YouTubeVideoInfo> {
        val streams = mutableListOf<YouTubeVideoInfo>()
        try {
            val json = extractPlayerResponseJson(html) ?: return streams
            val playerResponse = JSONObject(json)
            val streamingData = playerResponse.optJSONObject("streamingData") ?: return streams
            parseFormats(streamingData.optJSONArray("formats"), title, streams)
            parseFormats(streamingData.optJSONArray("adaptiveFormats"), title, streams)
        } catch (_: Exception) { }
        return streams
    }

    private fun extractPlayerResponseJson(html: String): String? {
        val markers = listOf("var ytInitialPlayerResponse = ", "ytInitialPlayerResponse = ")
        for (marker in markers) {
            val idx = html.indexOf(marker)
            if (idx == -1) continue
            val jsonStart = idx + marker.length
            if (jsonStart >= html.length || html[jsonStart] != '{') continue
            return extractBalancedJson(html, jsonStart)
        }
        return null
    }

    private fun extractBalancedJson(text: String, start: Int): String? {
        var depth = 0
        var i = start
        while (i < text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) return text.substring(start, i + 1) }
                '"' -> { i++; while (i < text.length && text[i] != '"') { if (text[i] == '\\') i++; i++ } }
            }
            i++
        }
        return null
    }

    private fun parseFormats(formats: org.json.JSONArray?, title: String, streams: MutableList<YouTubeVideoInfo>) {
        if (formats == null) return
        for (i in 0 until formats.length()) {
            try {
                val format = formats.getJSONObject(i)
                val url = format.optString("url", "")
                if (url.isEmpty()) continue
                val mimeType = format.optString("mimeType", "video/mp4")
                val quality = format.optString("qualityLabel", format.optString("quality", "unknown"))
                val isAudio = mimeType.startsWith("audio/")
                val ext = when {
                    mimeType.contains("webm") -> "webm"
                    isAudio -> "m4a"
                    else -> "mp4"
                }
                val displayQuality = if (isAudio) "audio-${format.optInt("audioBitrate", 0)}kbps" else quality
                val contentLength = format.optLong("contentLength", 0L)
                streams.add(YouTubeVideoInfo(title, url, mimeType.substringBefore(";"), displayQuality, ext, contentLength))
            } catch (_: Exception) { }
        }
    }
}
