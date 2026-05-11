package com.turbodownloader.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class YouTubeVideoInfo(
    val title: String,
    val directUrl: String,
    val mimeType: String,
    val quality: String,
    val fileExtension: String
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

                val infoUrl = "https://www.youtube.com/watch?v=$videoId"
                val request = Request.Builder()
                    .url(infoUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

                val title = extractTitle(body) ?: "youtube_${videoId}"
                val streams = extractStreams(body, title)

                if (streams.isEmpty()) {
                    Result.failure(Exception("Could not extract video streams. Video may be restricted."))
                } else {
                    Result.success(streams)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun extractTitle(html: String): String? {
        val titlePattern = Pattern.compile("<title>(.*?)(?:\\s*-\\s*YouTube)?</title>")
        val matcher = titlePattern.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
                ?.replace("[^a-zA-Z0-9\\s\\-_]".toRegex(), "")
                ?.replace("\\s+".toRegex(), "_")
                ?.take(100)
        }
        return null
    }

    private fun extractStreams(html: String, title: String): List<YouTubeVideoInfo> {
        val streams = mutableListOf<YouTubeVideoInfo>()

        try {
            val playerResponsePattern = Pattern.compile("var ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\});")
            val matcher = playerResponsePattern.matcher(html)

            if (!matcher.find()) {
                val altPattern = Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.*?\\});")
                val altMatcher = altPattern.matcher(html)
                if (!altMatcher.find()) return streams
                parsePlayerResponse(altMatcher.group(1) ?: return streams, title, streams)
            } else {
                parsePlayerResponse(matcher.group(1) ?: return streams, title, streams)
            }
        } catch (_: Exception) {
        }

        return streams
    }

    private fun parsePlayerResponse(json: String, title: String, streams: MutableList<YouTubeVideoInfo>) {
        try {
            val playerResponse = JSONObject(json)
            val streamingData = playerResponse.optJSONObject("streamingData") ?: return

            val formats = streamingData.optJSONArray("formats")
            if (formats != null) {
                for (i in 0 until formats.length()) {
                    val format = formats.getJSONObject(i)
                    val url = format.optString("url", "")
                    if (url.isNotEmpty()) {
                        val mimeType = format.optString("mimeType", "video/mp4")
                        val quality = format.optString("qualityLabel", format.optString("quality", "unknown"))
                        val ext = if (mimeType.contains("webm")) "webm" else "mp4"
                        streams.add(
                            YouTubeVideoInfo(
                                title = title,
                                directUrl = url,
                                mimeType = mimeType.split(";").first(),
                                quality = quality,
                                fileExtension = ext
                            )
                        )
                    }
                }
            }

            val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.getJSONObject(i)
                    val url = format.optString("url", "")
                    if (url.isNotEmpty()) {
                        val mimeType = format.optString("mimeType", "video/mp4")
                        val quality = format.optString("qualityLabel", format.optString("quality", "unknown"))
                        val ext = if (mimeType.contains("webm")) "webm" else if (mimeType.contains("audio")) "m4a" else "mp4"
                        val isAudio = mimeType.startsWith("audio/")
                        streams.add(
                            YouTubeVideoInfo(
                                title = title,
                                directUrl = url,
                                mimeType = mimeType.split(";").first(),
                                quality = if (isAudio) "audio-${format.optInt("audioBitrate", 0)}kbps" else quality,
                                fileExtension = ext
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
