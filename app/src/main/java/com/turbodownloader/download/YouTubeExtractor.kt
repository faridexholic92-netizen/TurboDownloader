package com.turbodownloader.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

import java.util.concurrent.TimeUnit
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
    private var initialized = false

    companion object {
        private val YOUTUBE_PATTERNS = listOf(
            Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([\\w-]{11})"),
            Pattern.compile("(?:https?://)?(?:m\\.)?youtube\\.com/watch\\?v=([\\w-]{11})"),
            Pattern.compile("(?:https?://)?youtu\\.be/([\\w-]{11})"),
            Pattern.compile("(?:https?://)?(?:www\\.)?youtube\\.com/shorts/([\\w-]{11})")
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

    @Synchronized
    private fun ensureInitialized() {
        if (initialized) return
        val httpClient = client.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        NewPipe.init(object : Downloader() {
            override fun execute(request: Request): Response {
                val data = request.dataToSend()
                val builder = okhttp3.Request.Builder()
                    .url(request.url())
                    .method(
                        request.httpMethod(),
                        if (data != null)
                            okhttp3.RequestBody.create(null, data)
                        else null
                    )

                val headers = request.headers()
                for ((key, values) in headers) {
                    for (value in values) {
                        builder.addHeader(key, value)
                    }
                }

                builder.header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
                )

                val response = httpClient.newCall(builder.build()).execute()
                val body = response.body?.string() ?: ""

                val responseHeaders = mutableMapOf<String, List<String>>()
                for (name in response.headers.names()) {
                    responseHeaders[name] = response.headers.values(name)
                }

                return Response(
                    response.code,
                    response.message,
                    responseHeaders,
                    body,
                    response.request.url.toString()
                )
            }
        })
        initialized = true
    }

    suspend fun extractVideoInfo(youtubeUrl: String): Result<List<YouTubeVideoInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val videoId = extractVideoId(youtubeUrl)
                    ?: return@withContext Result.failure(
                        Exception("Invalid YouTube URL. Paste a valid link like:\nhttps://www.youtube.com/watch?v=xxxxx\nhttps://youtu.be/xxxxx")
                    )

                ensureInitialized()

                val url = "https://www.youtube.com/watch?v=$videoId"
                val extractor = ServiceList.YouTube.getStreamExtractor(url)
                extractor.fetchPage()

                val title = try {
                    extractor.name
                        .replace("[^a-zA-Z0-9\\s\\-_]".toRegex(), "")
                        .replace("\\s+".toRegex(), "_")
                        .take(100)
                        .ifBlank { "youtube_$videoId" }
                } catch (_: Exception) {
                    "youtube_$videoId"
                }

                val streams = mutableListOf<YouTubeVideoInfo>()

                // Video streams
                try {
                    val videoStreams = extractor.videoStreams
                    for (stream in videoStreams) {
                        val streamUrl = stream.content ?: continue
                        if (streamUrl.isEmpty()) continue
                        val format = stream.format
                        val ext = format?.suffix ?: "mp4"
                        val mime = format?.mimeType ?: "video/mp4"
                        val quality = stream.resolution ?: "unknown"

                        streams.add(
                            YouTubeVideoInfo(
                                title = title,
                                directUrl = streamUrl,
                                mimeType = mime,
                                quality = quality,
                                fileExtension = ext
                            )
                        )
                    }
                } catch (_: Exception) {}

                // Video-only streams
                try {
                    val videoOnlyStreams = extractor.videoOnlyStreams
                    for (stream in videoOnlyStreams) {
                        val streamUrl = stream.content ?: continue
                        if (streamUrl.isEmpty()) continue
                        val format = stream.format
                        val ext = format?.suffix ?: "mp4"
                        val mime = format?.mimeType ?: "video/mp4"
                        val quality = "${stream.resolution ?: "unknown"} (video only)"

                        streams.add(
                            YouTubeVideoInfo(
                                title = title,
                                directUrl = streamUrl,
                                mimeType = mime,
                                quality = quality,
                                fileExtension = ext
                            )
                        )
                    }
                } catch (_: Exception) {}

                // Audio streams
                try {
                    val audioStreams = extractor.audioStreams
                    for (stream in audioStreams) {
                        val streamUrl = stream.content ?: continue
                        if (streamUrl.isEmpty()) continue
                        val format = stream.format
                        val ext = format?.suffix ?: "m4a"
                        val mime = format?.mimeType ?: "audio/mp4"
                        val bitrate = stream.averageBitrate
                        val quality = "Audio ${bitrate}kbps"

                        streams.add(
                            YouTubeVideoInfo(
                                title = title,
                                directUrl = streamUrl,
                                mimeType = mime,
                                quality = quality,
                                fileExtension = ext
                            )
                        )
                    }
                } catch (_: Exception) {}

                if (streams.isEmpty()) {
                    Result.failure(Exception("No downloadable streams found. Video may be restricted, private, or age-gated."))
                } else {
                    Result.success(streams)
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("captcha", ignoreCase = true) == true ->
                        "YouTube is requesting CAPTCHA verification. Try again later."
                    e.message?.contains("not available", ignoreCase = true) == true ->
                        "Video is not available. It may be private or region-restricted."
                    else -> "YouTube extraction failed: ${e.message}"
                }
                Result.failure(Exception(msg))
            }
        }
}
