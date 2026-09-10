package com.soundtube.app.data

import android.util.Log
import com.soundtube.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EpisodeInfo(
    val seriesTitle: String,
    val episodeNumber: Int,
    val rawTag: String
)

object EpisodeManager {
    private const val TAG = "EpisodeManager"

    private val EPISODE_PATTERNS = listOf(
        // "Tập 30", "TẬP 1", "Tập: 30", "Chương 120", "Hồi 15", "Phần 2", "Số 1", "Part 3", "Ep 05"
        Regex("""(?iu)(?:tập|t[aâậ]p|chương|chuong|hồi|hoi|phần|phan|số|so|part|ep|episode)\s*[:#\-–]?\s*(\d+)"""),
        // "Truyện ABC - 30", "Truyện ABC | 30"
        Regex("""(?iu)[\-–|]\s*(?:tập|t[aâậ]p|số|so)?\s*(\d+)\s*$"""),
        // "T.30", "Ep.30", "Vol.30"
        Regex("""(?iu)\b(?:T|Ep|Vol)\.?\s*(\d+)\b""")
    )

    /**
     * Phân tích số tập và tên bộ truyện từ tiêu đề video
     */
    fun parseEpisode(title: String): EpisodeInfo? {
        if (title.isBlank()) return null

        for (pattern in EPISODE_PATTERNS) {
            val match = pattern.find(title)
            if (match != null) {
                val epNum = match.groupValues[1].toIntOrNull()
                if (epNum != null && epNum > 0) {
                    val rawTag = match.value
                    // Xóa phần "Tập X" khỏi tiêu đề để lấy tên truyện gốc
                    var cleanTitle = title.substring(0, match.range.first).trim()
                    cleanTitle = cleanTitle.trimEnd('-', '–', '|', ':', ' ', '#', '[', '(', '{')
                    if (cleanTitle.length < 3) {
                        // Nếu phần trước quá ngắn, thử lấy phần sau
                        cleanTitle = title.substring(match.range.last + 1).trim()
                        cleanTitle = cleanTitle.trimStart('-', '–', '|', ':', ' ', '#', ']', ')', '}')
                    }
                    if (cleanTitle.isBlank()) cleanTitle = title

                    return EpisodeInfo(
                        seriesTitle = cleanTitle,
                        episodeNumber = epNum,
                        rawTag = rawTag
                    )
                }
            }
        }
        return null
    }

    /**
     * Tìm kiếm tập tiếp theo (Tập N+1) từ cùng kênh phát hành
     */
    suspend fun findNextEpisode(currentSong: Song): Song? = withContext(Dispatchers.IO) {
        val info = parseEpisode(currentSong.title) ?: return@withContext null
        val targetEp = info.episodeNumber + 1

        val query = "${currentSong.uploaderName} ${info.seriesTitle} tập $targetEp"
        Log.d(TAG, "Searching for next episode with query: $query")

        try {
            val searchResults = YouTubeExtractor.search(query)
            if (searchResults.isEmpty()) return@withContext null

            // 1. Ưu tiên video có cùng người đăng (uploaderName) và có số tập khớp chính xác targetEp
            val exactMatch = searchResults.firstOrNull { candidate ->
                candidate.id != currentSong.id &&
                candidate.uploaderName.equals(currentSong.uploaderName, ignoreCase = true) &&
                parseEpisode(candidate.title)?.episodeNumber == targetEp
            }
            if (exactMatch != null) return@withContext exactMatch

            // 2. Nếu không khớp tuyệt đối, tìm video có số tập khớp targetEp
            val epMatch = searchResults.firstOrNull { candidate ->
                candidate.id != currentSong.id &&
                parseEpisode(candidate.title)?.episodeNumber == targetEp
            }
            if (epMatch != null) return@withContext epMatch

            // 3. Nếu vẫn chưa có, lấy kết quả đầu tiên khác với bài hiện tại
            return@withContext searchResults.firstOrNull { it.id != currentSong.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding next episode: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Tìm danh sách các tập của bộ truyện từ cùng kênh phát hành
     */
    suspend fun findSeriesEpisodes(currentSong: Song): List<Song> = withContext(Dispatchers.IO) {
        val info = parseEpisode(currentSong.title) ?: return@withContext listOf(currentSong)
        val query = "${currentSong.uploaderName} ${info.seriesTitle}"

        try {
            val results = YouTubeExtractor.search(query)
            val seriesList = results.filter { candidate ->
                candidate.uploaderName.equals(currentSong.uploaderName, ignoreCase = true) ||
                candidate.title.contains(info.seriesTitle.take(15), ignoreCase = true)
            }.toMutableList()

            if (seriesList.none { it.id == currentSong.id }) {
                seriesList.add(currentSong)
            }

            // Sắp xếp theo số tập tăng dần (Tập 1, 2, 3...)
            seriesList.sortBy { parseEpisode(it.title)?.episodeNumber ?: 9999 }
            return@withContext seriesList
        } catch (e: Exception) {
            Log.e(TAG, "Error finding series episodes: ${e.message}", e)
            return@withContext listOf(currentSong)
        }
    }
}
