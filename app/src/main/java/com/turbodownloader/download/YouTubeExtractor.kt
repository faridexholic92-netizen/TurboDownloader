package com.turbodownloader.download

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class YouTubeVideoInfo(
    val title: String,
    val directUrl: String,
    val mimeType: String,
    val quality: String,
    val fileExtension: String,
    val fileSize: Long = 0L,
    val formatId: String = ""
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
                    ?: return@withContext Result.failure(
                        Exception("Invalid YouTube URL. Paste a valid link like:\nhttps://www.youtube.com/watch?v=xxxxx\nhttps://youtu.be/xxxxx")
                    )

                val url = "https://www.youtube.com/watch?v=$videoId"

                // Use yt-dlp to get video info (same as user's Python: yt-dlp -j URL)
                val request = YoutubeDLRequest(url)
                request.addOption("--dump-json")
                request.addOption("--no-playlist")

                val response = YoutubeDL.getInstance().execute(request)
                val jsonStr = response.out

                if (jsonStr.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("No video info returned from yt-dlp"))
                }

                val json = org.json.JSONObject(jsonStr)
                val title = json.optString("title", "youtube_$videoId")
                    .replace("[^a-zA-Z0-9\\s\\-_]".toRegex(), "")
                    .replace("\\s+".toRegex(), "_")
                    .take(100)
                    .ifBlank { "youtube_$videoId" }

                val streams = mutableListOf<YouTubeVideoInfo>()
                val formats = json.optJSONArray("formats")

                if (formats != null) {
                    for (i in 0 until formats.length()) {
                        try {
                            val format = formats.getJSONObject(i)
                            val formatUrl = format.optString("url", "")
                            if (formatUrl.isEmpty()) continue

                            val formatId = format.optString("format_id", "")
                            val ext = format.optString("ext", "mp4")
                            val filesize = format.optLong("filesize", format.optLong("filesize_approx", 0L))
                            val height = format.optInt("height", 0)
                            val vcodec = format.optString("vcodec", "none")
                            val acodec = format.optString("acodec", "none")

                            val isVideo = vcodec != "none" && vcodec.isNotEmpty()
                            val isAudio = acodec != "none" && acodec.isNotEmpty()

                            if (!isVideo && !isAudio) continue

                            val quality: String
                            val mimeType: String

                            if (isVideo && isAudio) {
                                quality = "${height}p"
                                mimeType = "video/$ext"
                            } else if (isVideo) {
                                quality = "${height}p (video only)"
                                mimeType = "video/$ext"
                            } else {
                                val abr = format.optInt("abr", format.optInt("tbr", 0))
                                quality = "Audio ${abr}kbps"
                                mimeType = "audio/$ext"
                            }

                            if (height > 0 || !isVideo) {
                                streams.add(
                                    YouTubeVideoInfo(
                                        title = title,
                                        directUrl = formatUrl,
                                        mimeType = mimeType,
                                        quality = quality,
                                        fileExtension = ext,
                                        fileSize = filesize,
                                        formatId = formatId
                                    )
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Sort: video+audio first (by height desc), then video-only (by height desc), then audio (by bitrate desc)
                val sorted = streams.sortedWith(compareByDescending<YouTubeVideoInfo> {
                    when {
                        it.mimeType.startsWith("video/") && !it.quality.contains("video only") -> 2
                        it.mimeType.startsWith("video/") -> 1
                        else -> 0
                    }
                }.thenByDescending {
                    Regex("(\\d+)").find(it.quality)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                })

                if (sorted.isEmpty()) {
                    Result.failure(Exception("No downloadable streams found. Video may be restricted or private."))
                } else {
                    Result.success(sorted)
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("Video unavailable", ignoreCase = true) == true ->
                        "Video is not available. It may be private or deleted."
                    e.message?.contains("Sign in", ignoreCase = true) == true ->
                        "This video requires sign-in. Try a different video."
                    e.message?.contains("confirm your age", ignoreCase = true) == true ->
                        "This video is age-restricted."
                    else -> "YouTube extraction failed: ${e.message?.take(200)}"
                }
                Result.failure(Exception(msg))
            }
        }
}
