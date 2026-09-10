package com.soundtube.app.data

import android.util.Log
import android.webkit.CookieManager
import com.soundtube.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class FeedPageResult(
    val videos: List<Song>,
    val continuationToken: String? = null
)

object YouTubeExtractor {
    private const val TAG = "YouTubeExtractor"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    // Các hạt giống ngẫu nhiên để truy vấn Live Trending từ Google/YouTube Suggest API
    val GENERAL_SEEDS = listOf(
        "a", "b", "c", "d", "e", "g", "h", "k", "l", "m", "n", "p", "q", "r", "s", "t", "v",
        "nhạc", "phim", "hài", "show", "vlog", "review", "khám phá", "triệu view", "thịnh hành", "mới nhất",
        "2 ngày 1 đêm", "running man", "gameshow", "ẩm thực", "du lịch", "khoa học", "hoạt hình"
    )

    val AUDIOBOOK_SEEDS = listOf(
        "truyện audio", "truyện ma", "sách nói", "tiểu thuyết audio", "truyện kiếm hiệp audio",
        "truyện tiên hiệp audio", "truyện trinh thám audio", "audiobook kinh doanh", "truyện nguyễn ngọc ngạn",
        "truyện đêm khuya", "truyện ngôn tình audio", "truyện ngắn audio", "truyện ma chú ba duy", "sách nói đắc nhân tâm"
    )

    val MUSIC_SEEDS = listOf(
        "nhạc", "nhạc remix", "mv vpop", "nhạc chill", "nhạc acoustic", "nhạc trẻ", "nhạc ballad",
        "lofi việt", "nhạc indie", "nhạc bolero", "nhạc edm", "karaoke nhạc trẻ", "live session nhạc", "mashup nhạc"
    )

    val PLAYLIST_SEEDS = listOf(
        "playlist", "tuyển tập bài hát", "nonstop remix", "album nhạc", "nhạc không lời thư giãn",
        "tổng hợp nhạc trẻ", "playlist acoustic", "liên khúc nhạc", "nhạc quán cafe", "nhạc ngủ ngon"
    )

    val PODCAST_SEEDS = listOf(
        "podcast", "tri kỷ cảm xúc", "vietcetera podcast", "sunhuyn podcast", "talkshow tâm sự",
        "podcast chữa lành", "podcast phát triển bản thân", "have a sip podcast", "dustin on the go"
    )

    val NEWS_SEEDS = listOf(
        "vtv24", "thời sự vtv1", "tin tức mới nhất", "bản tin 24h", "tin tức thời sự hôm nay"
    )

    val GAMING_SEEDS = listOf(
        "gameplay việt nam", "liên quân mobile", "review game", "stream game hài hước", "highlight game"
    )

    /**
     * Lấy từ khóa xu hướng thực tế theo thời gian thực từ Google/YouTube Live Suggest API
     * Giúp hệ thống hoàn toàn tự động, ngẫu nhiên hóa và không bao giờ bị cố định chủ đề.
     */
    suspend fun fetchLiveTrendingSuggestions(seed: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val encodedSeed = URLEncoder.encode(seed, "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=vi&gl=vn&q=$encodedSeed"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val jsonArr = JSONArray(bodyStr)
                if (jsonArr.length() > 1) {
                    val suggestionsArr = jsonArr.optJSONArray(1)
                    if (suggestionsArr != null) {
                        val list = mutableListOf<String>()
                        for (i in 0 until suggestionsArr.length()) {
                            val text = suggestionsArr.optString(i, "")
                            if (text.isNotBlank()) list.add(text)
                        }
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi lấy YouTube Suggest: ${e.message}")
        }
        return@withContext emptyList()
    }

    /**
     * Lấy cookie YouTube từ WebView để đồng bộ trạng thái đăng nhập
     */
    private fun getYouTubeCookie(): String? {
        return try {
            CookieManager.getInstance().getCookie("https://www.youtube.com")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Tìm kiếm một trang kèm continuation token để hỗ trợ Load More vô tận
     */
    suspend fun searchPage(query: String, continuationToken: String? = null): FeedPageResult = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.01.00")
                        put("hl", "vi")
                        put("gl", "VN")
                    })
                })
                if (!continuationToken.isNullOrBlank()) {
                    put("continuation", continuationToken)
                } else {
                    put("query", query)
                }
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val requestBuilder = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search")
                .post(requestBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/")

            val cookie = getYouTubeCookie()
            if (!cookie.isNullOrBlank()) {
                requestBuilder.header("Cookie", cookie)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                return@withContext extractVideosAndContinuation(rootJson)
            } else {
                Log.e(TAG, "Search HTTP error: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search exception: ${e.message}", e)
        }
        return@withContext FeedPageResult(emptyList(), null)
    }

    /**
     * Hàm tìm kiếm cơ bản trả về danh sách Song (Tương thích ngược)
     */
    suspend fun search(query: String): List<Song> {
        return searchPage(query).videos
    }

    /**
     * Phân tích JSON YouTube Innertube để trích xuất danh sách Song và continuationToken
     */
    fun extractVideosAndContinuation(rootJson: JSONObject): FeedPageResult {
        val results = mutableListOf<Song>()
        var nextToken: String? = null

        // 1. Initial Search: rootJson -> contents -> twoColumnSearchResultsRenderer -> primaryContents -> sectionListRenderer
        val sectionListContents = rootJson.optJSONObject("contents")
            ?.optJSONObject("twoColumnSearchResultsRenderer")
            ?.optJSONObject("primaryContents")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

        if (sectionListContents != null) {
            for (i in 0 until sectionListContents.length()) {
                val sec = sectionListContents.optJSONObject(i) ?: continue

                // Token ở cuối sectionListRenderer
                val contItem = sec.optJSONObject("continuationItemRenderer")
                if (contItem != null) {
                    val token = contItem.optJSONObject("continuationEndpoint")
                        ?.optJSONObject("continuationCommand")
                        ?.optString("token")
                    if (!token.isNullOrBlank()) {
                        nextToken = token
                    }
                }

                // Video items trong itemSectionRenderer
                val itemSection = sec.optJSONObject("itemSectionRenderer") ?: continue
                val items = itemSection.optJSONArray("contents") ?: continue
                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue
                    val v = item.optJSONObject("videoRenderer")
                    if (v != null) {
                        val song = parseVideoRenderer(v)
                        if (song != null) results.add(song)
                    }
                    val innerCont = item.optJSONObject("continuationItemRenderer")
                    if (innerCont != null) {
                        val token = innerCont.optJSONObject("continuationEndpoint")
                            ?.optJSONObject("continuationCommand")
                            ?.optString("token")
                        if (!token.isNullOrBlank()) {
                            nextToken = token
                        }
                    }
                }
            }
        }

        // 2. Continuation Search: rootJson -> onResponseReceivedCommands -> appendContinuationItemsAction
        val commands = rootJson.optJSONArray("onResponseReceivedCommands")
        if (commands != null) {
            for (i in 0 until commands.length()) {
                val cmd = commands.optJSONObject(i) ?: continue
                val appendAction = cmd.optJSONObject("appendContinuationItemsAction") ?: continue
                val contItems = appendAction.optJSONArray("continuationItems") ?: continue
                for (j in 0 until contItems.length()) {
                    val item = contItems.optJSONObject(j) ?: continue

                    // Trực tiếp videoRenderer
                    val v = item.optJSONObject("videoRenderer")
                    if (v != null) {
                        val song = parseVideoRenderer(v)
                        if (song != null) results.add(song)
                    }

                    // itemSectionRenderer bọc ngoài
                    val itemSec = item.optJSONObject("itemSectionRenderer")
                    if (itemSec != null) {
                        val contents = itemSec.optJSONArray("contents")
                        if (contents != null) {
                            for (k in 0 until contents.length()) {
                                val inner = contents.optJSONObject(k) ?: continue
                                val innerV = inner.optJSONObject("videoRenderer")
                                if (innerV != null) {
                                    val song = parseVideoRenderer(innerV)
                                    if (song != null) results.add(song)
                                }
                            }
                        }
                    }

                    // Token tiếp theo trong continuationItems
                    val cont = item.optJSONObject("continuationItemRenderer")
                    if (cont != null) {
                        val token = cont.optJSONObject("continuationEndpoint")
                            ?.optJSONObject("continuationCommand")
                            ?.optString("token")
                        if (!token.isNullOrBlank()) {
                            nextToken = token
                        }
                    }
                }
            }
        }

        return FeedPageResult(videos = results, continuationToken = nextToken)
    }

    private fun parseVideoRenderer(v: JSONObject): Song? {
        val videoId = v.optString("videoId", "")
        if (videoId.isBlank()) return null

        // Tiêu đề
        var title = "Video YouTube"
        val titleObj = v.optJSONObject("title")
        if (titleObj != null) {
            val runs = titleObj.optJSONArray("runs")
            title = if (runs != null && runs.length() > 0) {
                runs.getJSONObject(0).optString("text", title)
            } else {
                titleObj.optString("simpleText", title)
            }
        }

        // Kênh / Tác giả
        var uploader = "YouTube"
        val ownerObj = v.optJSONObject("ownerText") ?: v.optJSONObject("longBylineText") ?: v.optJSONObject("shortBylineText")
        if (ownerObj != null) {
            val runs = ownerObj.optJSONArray("runs")
            if (runs != null && runs.length() > 0) {
                uploader = runs.getJSONObject(0).optString("text", uploader)
            }
        }

        // Avatar kênh
        var avatarUrl: String? = null
        val channelThumbs = v.optJSONObject("channelThumbnailSupportedRenderers")
            ?.optJSONObject("channelThumbnailWithLinkRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        if (channelThumbs != null && channelThumbs.length() > 0) {
            avatarUrl = channelThumbs.getJSONObject(channelThumbs.length() - 1).optString("url")
        }

        // Lượt xem
        var viewCount: String? = null
        val shortViewObj = v.optJSONObject("shortViewCountText")
        if (shortViewObj != null) {
            val runs = shortViewObj.optJSONArray("runs")
            viewCount = if (runs != null && runs.length() > 0) {
                runs.getJSONObject(0).optString("text")
            } else {
                shortViewObj.optString("simpleText")
            }
        }
        if (viewCount.isNullOrBlank()) {
            viewCount = v.optJSONObject("viewCountText")?.optString("simpleText")
        }

        // Thời gian đăng tải
        val publishedTime = v.optJSONObject("publishedTimeText")?.optString("simpleText")

        // Thời lượng
        var durationSec = 0L
        var durationStr: String? = null
        val lengthObj = v.optJSONObject("lengthText")
        if (lengthObj != null) {
            durationStr = lengthObj.optString("simpleText", "")
            durationSec = parseDurationTextToSeconds(durationStr)
        }

        // Thumbnail HD
        val thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        // Đọc tiến trình xem nếu có (red bar trên thumbnail)
        var watchedPercent = 0
        val overlays = v.optJSONArray("thumbnailOverlays")
        if (overlays != null) {
            for (idx in 0 until overlays.length()) {
                val ov = overlays.optJSONObject(idx) ?: continue
                val resumeRenderer = ov.optJSONObject("thumbnailOverlayResumePlaybackRenderer")
                if (resumeRenderer != null) {
                    val p = resumeRenderer.optInt("percentDurationWatched", 0)
                    if (p in 1..99) watchedPercent = p
                }
                val bottomOverlay = ov.optJSONObject("thumbnailBottomOverlayViewModel")
                if (bottomOverlay != null) {
                    val badges = bottomOverlay.optJSONArray("badges")
                    if (badges != null && badges.length() > 0 && durationSec == 0L) {
                        for (bIdx in 0 until badges.length()) {
                            val badgeText = badges.optJSONObject(bIdx)
                                ?.optJSONObject("thumbnailBadgeViewModel")
                                ?.optString("text")
                            if (!badgeText.isNullOrBlank()) {
                                durationStr = badgeText
                                durationSec = parseDurationTextToSeconds(badgeText)
                                break
                            }
                        }
                    }
                    val pb = bottomOverlay.optJSONObject("progressBar")?.optJSONObject("thumbnailOverlayProgressBarViewModel")
                    val p = pb?.optInt("startPercent", 0) ?: 0
                    if (p in 1..99) watchedPercent = p
                }
            }
        }
        if (watchedPercent > 0 && durationSec > 0) {
            val posMs = (durationSec * 1000L * watchedPercent) / 100
            PlaybackProgressManager.saveProgress(videoId, posMs, durationSec * 1000L)
            Log.d(TAG, "Đã khôi phục tiến trình dừng (videoRenderer) cho $videoId: $posMs ms ($watchedPercent%)")
        }

        return Song(
            id = videoId,
            title = title,
            uploaderName = uploader,
            durationSeconds = durationSec,
            thumbnailUrl = thumb,
            channelAvatarUrl = avatarUrl,
            viewCountText = viewCount,
            publishedTimeText = publishedTime,
            durationText = durationStr
        )
    }

    private fun parseLockupViewModel(lockup: JSONObject): Song? {
        var videoId = lockup.optString("contentId", "")

        // Nếu contentId rỗng, lấy từ thumbnail URL
        if (videoId.isBlank()) {
            val thumbSources = lockup.optJSONObject("contentImage")
                ?.optJSONObject("thumbnailViewModel")
                ?.optJSONObject("image")
                ?.optJSONArray("sources")
            if (thumbSources != null && thumbSources.length() > 0) {
                val url = thumbSources.getJSONObject(0).optString("url", "")
                val m = Regex("vi/([a-zA-Z0-9_-]{11})/").find(url)
                if (m != null) videoId = m.groupValues[1]
            }
        }
        if (videoId.isBlank()) return null

        val meta = lockup.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")
        val title = meta?.optJSONObject("title")?.optString("content", "") ?: "Video YouTube"

        // Tác giả / Kênh
        var uploader = "YouTube"
        var avatarUrl: String? = null

        val avatarSources = meta?.optJSONObject("image")
            ?.optJSONObject("decoratedAvatarViewModel")
            ?.optJSONObject("avatar")
            ?.optJSONObject("avatarViewModel")
            ?.optJSONObject("image")
            ?.optJSONArray("sources")
        if (avatarSources != null && avatarSources.length() > 0) {
            avatarUrl = avatarSources.getJSONObject(0).optString("url", null)
        }

        val a11y = meta?.optJSONObject("image")
            ?.optJSONObject("decoratedAvatarViewModel")
            ?.optString("a11yLabel", "") ?: ""
        if (a11y.isNotBlank()) {
            uploader = a11y.replace(Regex("^(?:Chuyển đến kênh|Go to channel)\\s+"), "")
        } else {
            // Lấy từ metadataRows
            val rows = meta?.optJSONObject("metadata")
                ?.optJSONObject("contentMetadataViewModel")
                ?.optJSONArray("metadataRows")
            if (rows != null && rows.length() > 0) {
                val parts = rows.optJSONObject(0)?.optJSONArray("metadataParts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.optJSONObject(0)?.optJSONObject("text")?.optString("content", "")
                    if (!text.isNullOrBlank()) uploader = text
                }
            }
        }

        // Thumbnail HD
        val thumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        // Overlays (Thời lượng + Thanh tiến trình đỏ)
        var durationSec = 0L
        var durationStr: String? = null
        var watchedPercent = 0

        val overlays = lockup.optJSONObject("contentImage")
            ?.optJSONObject("thumbnailViewModel")
            ?.optJSONArray("overlays")
        if (overlays != null) {
            for (idx in 0 until overlays.length()) {
                val ov = overlays.optJSONObject(idx) ?: continue

                // 1. Dạng hiện đại của YouTube Web: thumbnailBottomOverlayViewModel
                val bottomOverlay = ov.optJSONObject("thumbnailBottomOverlayViewModel")
                if (bottomOverlay != null) {
                    // Thời lượng (badges)
                    val badges = bottomOverlay.optJSONArray("badges")
                    if (badges != null && badges.length() > 0 && durationSec == 0L) {
                        for (bIdx in 0 until badges.length()) {
                            val badgeText = badges.optJSONObject(bIdx)
                                ?.optJSONObject("thumbnailBadgeViewModel")
                                ?.optString("text")
                            if (!badgeText.isNullOrBlank()) {
                                durationStr = badgeText
                                durationSec = parseDurationTextToSeconds(badgeText)
                                break
                            }
                        }
                    }
                    // Tiến trình xem dở trên web (% đã xem: startPercent)
                    val progressBar = bottomOverlay.optJSONObject("progressBar")
                        ?.optJSONObject("thumbnailOverlayProgressBarViewModel")
                    val startPercent = progressBar?.optInt("startPercent", 0) ?: 0
                    if (startPercent in 1..99) {
                        watchedPercent = startPercent
                    }
                }

                // 2. Dạng truyền thống: thumbnailOverlayTimeStatusRenderer
                val timeBadge = ov.optJSONObject("thumbnailOverlayTimeStatusRenderer")
                    ?.optJSONObject("text")?.optString("simpleText", "")
                if (!timeBadge.isNullOrBlank() && durationSec == 0L) {
                    durationStr = timeBadge
                    durationSec = parseDurationTextToSeconds(timeBadge)
                }

                // 3. Dạng truyền thống: thumbnailOverlayResumePlaybackRenderer
                val resume = ov.optJSONObject("thumbnailOverlayResumePlaybackRenderer")
                if (resume != null) {
                    val percent = resume.optInt("percentDurationWatched", 0)
                    if (percent in 1..99) {
                        watchedPercent = percent
                    }
                }
            }
        }

        // Lưu tiến trình xem vào PlaybackProgressManager nếu có
        if (watchedPercent > 0 && durationSec > 0) {
            val posMs = (durationSec * 1000L * watchedPercent) / 100
            PlaybackProgressManager.saveProgress(videoId, posMs, durationSec * 1000L)
            Log.d(TAG, "Đã khôi phục vị trí dừng trên web (lockupViewModel) cho $videoId: $posMs ms ($watchedPercent%) / $durationSec s")
        }

        return Song(
            id = videoId,
            title = title,
            uploaderName = uploader,
            durationSeconds = durationSec,
            thumbnailUrl = thumb,
            channelAvatarUrl = avatarUrl,
            durationText = durationStr
        )
    }

    /**
     * Lấy truy vấn ngẫu nhiên từ Live Trending Suggestions của Google/YouTube
     */
    private suspend fun getRandomLiveQuery(seeds: List<String>): String {
        val randomSeed = seeds.random()
        val liveSuggestions = fetchLiveTrendingSuggestions(randomSeed)
        return if (liveSuggestions.isNotEmpty()) {
            liveSuggestions.random()
        } else {
            randomSeed
        }
    }

    /**
     * Thuật toán Đề xuất & Bảng tin chuyên biệt chuẩn YouTube (Dynamic Feed Engine)
     * - Hoàn toàn Random qua Live YouTube Suggest API (không dùng danh sách cố định tĩnh)
     * - Hòa trộn ngẫu nhiên nhiều luồng đề xuất thịnh hành thực tế
     * - Loại trừ các video đã xem (excludeIds) để không bao giờ bị lặp lại
     * - Hỗ trợ Load More mượt mà qua continuationToken hoặc Live Suggestion fallback
     */
    suspend fun getFeedForCategory(
        category: String,
        userHistory: List<Song> = emptyList(),
        continuationToken: String? = null,
        poolIndex: Int = 0,
        excludeIds: Set<String> = emptySet()
    ): FeedPageResult = coroutineScope {
        // 1. Nếu đang Load More bằng token của YouTube
        if (!continuationToken.isNullOrBlank()) {
            val result = searchPage(query = "", continuationToken = continuationToken)
            val filtered = result.videos.filterNot { it.id in excludeIds }
            if (filtered.isNotEmpty()) {
                return@coroutineScope FeedPageResult(filtered, result.continuationToken)
            }
        }

        val seeds = when (category) {
            "Truyện / Sách nói" -> AUDIOBOOK_SEEDS
            "Âm nhạc" -> MUSIC_SEEDS
            "Danh sách kết hợp" -> PLAYLIST_SEEDS
            "Podcast" -> PODCAST_SEEDS
            "Tin tức" -> NEWS_SEEDS
            "Trò chơi" -> GAMING_SEEDS
            else -> GENERAL_SEEDS
        }

        // 2. Nếu là thẻ "Tất cả": Lấy ngẫu nhiên 2 chủ đề Live từ YouTube và hòa trộn song song
        if (category == "Tất cả") {
            // Lấy 2 từ khóa ngẫu nhiên từ Live Suggest
            val query1 = getRandomLiveQuery(seeds)
            val query2 = getRandomLiveQuery(seeds)

            val def1 = async { searchPage(query1) }
            val def2 = async { searchPage(query2) }

            val favoriteChannels = userHistory.map { it.uploaderName }
                .filter { it.isNotBlank() && it != "YouTube" }
                .distinct()

            val favDef = if (favoriteChannels.isNotEmpty()) {
                val favChannel = favoriteChannels.random()
                async { searchPage("$favChannel mới nhất") }
            } else null

            val res1 = def1.await()
            val res2 = def2.await()
            val favRes = favDef?.await()

            // Hòa trộn đan xen so le giữa các chủ đề
            val combined = mutableListOf<Song>()
            val l1 = res1.videos
            val l2 = res2.videos
            val lf = favRes?.videos ?: emptyList()

            var i1 = 0
            var i2 = 0
            var ifav = 0

            while (i1 < l1.size || i2 < l2.size || ifav < lf.size) {
                if (ifav < lf.size) combined.add(lf[ifav++])
                if (i1 < l1.size) combined.add(l1[i1++])
                if (i2 < l2.size) combined.add(l2[i2++])
            }

            // Loại bỏ video đã hiển thị trong phiên (excludeIds) + loại bỏ video CCTV/live stream > 6 tiếng
            var valid = combined
                .filterNot { it.id in excludeIds }
                .filter { it.durationSeconds in 1..21600 || it.durationText != null && it.durationSeconds <= 21600 }
                .distinctBy { it.id }

            // Nếu sau khi loại trừ còn ít hơn 10 video, lấy thêm 1 từ khóa ngẫu nhiên nữa
            if (valid.size < 10) {
                val query3 = getRandomLiveQuery(seeds)
                val res3 = searchPage(query3)
                val more = (valid + res3.videos.filterNot { it.id in excludeIds }).distinctBy { it.id }
                return@coroutineScope FeedPageResult(
                    videos = more,
                    continuationToken = res1.continuationToken ?: res2.continuationToken
                )
            }

            return@coroutineScope FeedPageResult(
                videos = valid,
                continuationToken = res1.continuationToken ?: res2.continuationToken
            )
        }

        // 3. Đối với các danh mục chuyên biệt ("Truyện / Sách nói", "Âm nhạc",...):
        val randomQuery = getRandomLiveQuery(seeds)
        val res = searchPage(randomQuery)
        val validVideos = res.videos.filterNot { it.id in excludeIds }

        // Nếu query này bị trùng nhiều với các video đã xem, lấy thêm 1 query ngẫu nhiên khác
        if (validVideos.size < 10) {
            val nextQuery = getRandomLiveQuery(seeds)
            val nextRes = searchPage(nextQuery)
            val merged = (validVideos + nextRes.videos.filterNot { it.id in excludeIds }).distinctBy { it.id }
            return@coroutineScope FeedPageResult(
                videos = merged,
                continuationToken = res.continuationToken ?: nextRes.continuationToken
            )
        }

        return@coroutineScope FeedPageResult(
            videos = validVideos,
            continuationToken = res.continuationToken
        )
    }

    suspend fun getShorts(): List<Song> {
        return search("#shorts việt nam hot nhất")
    }

    private fun parseDurationTextToSeconds(text: String): Long {
        if (text.isBlank()) return 0L
        val parts = text.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0L
        }
    }

    private fun getSapisidHash(sapisid: String, origin: String = "https://www.youtube.com"): String {
        val timestamp = System.currentTimeMillis() / 1000
        val input = "$timestamp $sapisid $origin"
        val md = MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${timestamp}_${hex}"
    }

    /**
     * Tải danh sách lịch sử xem thực tế từ tài khoản YouTube chính thức (FEhistory)
     */
    suspend fun fetchYouTubeWatchHistory(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val cookies = getYouTubeCookie() ?: return@withContext emptyList()
            val sapisidRegex = Regex("(?:__Secure-3PAPISID|__Secure-1PAPISID|SAPISID)=([^;\\s]+)")
            val sapisid = sapisidRegex.find(cookies)?.groupValues?.get(1)?.trim()

            val jsonPayload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20240101.01.00")
                        put("hl", "vi")
                        put("gl", "VN")
                    })
                })
                put("browseId", "FEhistory")
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val reqBuilder = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/browse")
                .post(requestBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/feed/history")
                .header("Cookie", cookies)
                .header("X-Youtube-Client-Name", "1")
                .header("X-Youtube-Client-Version", "2.20240101.01.00")

            if (!sapisid.isNullOrBlank()) {
                reqBuilder.header("Authorization", getSapisidHash(sapisid))
                reqBuilder.header("X-Origin", "https://www.youtube.com")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val bodyStr = response.body?.string() ?: ""
            Log.d(TAG, "FEhistory response HTTP ${response.code}, size: ${bodyStr.length}")

            if (response.isSuccessful && bodyStr.isNotEmpty()) {
                val root = JSONObject(bodyStr)
                val results = mutableListOf<Song>()
                val seenIds = mutableSetOf<String>()

                fun extractFromObject(obj: JSONObject) {
                    if (obj.has("videoRenderer")) {
                        val v = obj.optJSONObject("videoRenderer")
                        if (v != null) {
                            parseVideoRenderer(v)?.let { song ->
                                if (seenIds.add(song.id)) {
                                    results.add(song)
                                }
                            }
                        }
                    }
                    if (obj.has("lockupViewModel")) {
                        val l = obj.optJSONObject("lockupViewModel")
                        if (l != null) {
                            parseLockupViewModel(l)?.let { song ->
                                if (seenIds.add(song.id)) {
                                    results.add(song)
                                }
                            }
                        }
                    }
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val child = obj.opt(key)
                        if (child is JSONObject) {
                            extractFromObject(child)
                        } else if (child is JSONArray) {
                            for (i in 0 until child.length()) {
                                val item = child.opt(i)
                                if (item is JSONObject) {
                                    extractFromObject(item)
                                }
                            }
                        }
                    }
                }

                val contents = root.optJSONObject("contents")
                if (contents != null) {
                    extractFromObject(contents)
                }
                Log.d(TAG, "Successfully extracted ${results.size} videos from YouTube Watch History")
                return@withContext results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching YouTube history: ${e.message}", e)
        }
        emptyList()
    }
}
