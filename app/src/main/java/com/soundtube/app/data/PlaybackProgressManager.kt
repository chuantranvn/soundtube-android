package com.soundtube.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object PlaybackProgressManager {
    private const val TAG = "PlaybackProgressManager"
    private const val PREFS_NAME = "soundtube_playback_progress"
    private const val KEY_PROGRESS_DATA = "progress_data"

    private var prefs: SharedPreferences? = null
    // Chỉ lưu tiến trình chính xác (tính bằng mili-giây) khi người dùng thực sự nghe trên app
    private val progressMap = mutableMapOf<String, Long>()

    // Lưu tỉ lệ phần trăm ước lượng từ YouTube Web (chỉ dùng để vẽ thanh đỏ trên thumbnail)
    private val remotePercentMap = mutableMapOf<String, Float>()

    // Lưu vị trí chính xác (mili-giây) từ YouTube Web (startTimeSeconds trong FEhistory)
    // Dùng để đồng bộ tiếp tục nghe từ YouTube Web sang App
    private val remoteExactProgressMap = mutableMapOf<String, Long>()

    private val _progressUpdated = MutableStateFlow(0L)
    val progressUpdated: StateFlow<Long> = _progressUpdated.asStateFlow()

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadAll()
        }
    }

    private fun loadAll() {
        val jsonStr = prefs?.getString(KEY_PROGRESS_DATA, null) ?: return
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                progressMap[key] = json.optLong(key, 0L)
            }
        } catch (_: Exception) {}
    }

    /**
     * Ghi nhận phần trăm thanh đỏ từ YouTube Web.
     * Chỉ dùng để hiển thị thanh đỏ trên thumbnail tham khảo, KHÔNG dùng để tự động seek video.
     */
    fun setRemoteWatchedPercent(videoId: String, percent: Int) {
        if (videoId.isBlank()) return
        if (percent in 1..99) {
            remotePercentMap[videoId] = (percent / 100f).coerceIn(0f, 1f)
            _progressUpdated.value = System.currentTimeMillis()
        }
    }

    /**
     * Ghi nhận vị trí xem dở CHÍNH XÁC từ YouTube Web (startTimeSeconds trong FEhistory response).
     * Dùng để đồng bộ tiếp tục nghe: YouTube Web → App.
     * Chỉ lưu nếu chưa có tiến trình nghe trên app (để ưu tiên local progress).
     */
    fun setRemoteExactProgress(videoId: String, startTimeSeconds: Int) {
        if (videoId.isBlank() || startTimeSeconds <= 0) return
        val posMs = startTimeSeconds * 1000L
        // Chỉ lưu remote progress nếu chưa có local progress (ưu tiên local)
        val localProgress = progressMap[videoId] ?: 0L
        if (localProgress <= 0L) {
            remoteExactProgressMap[videoId] = posMs
            Log.d(TAG, "Ghi nhận vị trí chính xác từ YouTube Web cho $videoId: ${startTimeSeconds}s (${posMs}ms)")
            _progressUpdated.value = System.currentTimeMillis()
        }
    }

    /**
     * Lưu vị trí phát thực tế của video tính bằng mili-giây (khi nghe trên app).
     */
    fun saveProgress(videoId: String, positionMs: Long, durationMs: Long) {
        if (videoId.isBlank() || positionMs < 0L) return

        // Nếu đã nghe gần hết (trên 95% thời lượng), xem như đã hoàn thành -> xóa tiến trình để lần sau nghe lại từ đầu
        if (durationMs > 0 && positionMs >= durationMs * 0.95f) {
            progressMap.remove(videoId)
            remoteExactProgressMap.remove(videoId)
            persist()
            _progressUpdated.value = System.currentTimeMillis()
            return
        }

        // Chỉ lưu khi đã nghe được hơn 3 giây
        if (positionMs > 3000L) {
            progressMap[videoId] = positionMs
            // Xóa remote progress vì local progress đã có (ưu tiên local)
            remoteExactProgressMap.remove(videoId)
            persist()
            _progressUpdated.value = System.currentTimeMillis()
        }
    }

    /**
     * Lấy vị trí đã dừng của video (mili-giây).
     * Ưu tiên: (1) Local progress (đã nghe trên app) → (2) Remote exact progress (startTimeSeconds từ YouTube Web)
     * Trả về 0L nếu không có bất kỳ tiến trình nào.
     */
    fun getProgress(videoId: String): Long {
        // 1. Ưu tiên tiến trình đã nghe thực tế trên app
        val localProgress = progressMap[videoId] ?: 0L
        if (localProgress > 0L) return localProgress

        // 2. Fallback: vị trí chính xác từ YouTube Web (startTimeSeconds)
        val remoteExact = remoteExactProgressMap[videoId] ?: 0L
        if (remoteExact > 0L) {
            Log.d(TAG, "Sử dụng vị trí từ YouTube Web cho $videoId: ${remoteExact / 1000}s")
            return remoteExact
        }

        return 0L
    }

    /**
     * Lấy tỉ lệ tiến trình đã nghe từ 0.0f đến 1.0f (phục vụ hiển thị thanh đỏ trên thumbnail).
     * Ưu tiên: local progress → remote exact progress → remote percent (thanh đỏ ước lượng).
     */
    fun getProgressRatio(videoId: String, durationSeconds: Long): Float {
        val durationMs = durationSeconds * 1000L

        // 1. Local progress
        val posMs = progressMap[videoId] ?: 0L
        if (durationMs > 0 && posMs > 0) {
            return (posMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }

        // 2. Remote exact progress (startTimeSeconds)
        val remoteExact = remoteExactProgressMap[videoId] ?: 0L
        if (durationMs > 0 && remoteExact > 0) {
            return (remoteExact.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }

        // 3. Remote percent (thanh đỏ ước lượng từ thumbnail)
        return remotePercentMap[videoId] ?: 0f
    }

    /**
     * Định dạng chuỗi phút dừng (ví dụ: "18:35").
     * Hiển thị khi có tiến trình nghe thực tế trên app HOẶC tiến trình chính xác từ YouTube Web.
     */
    fun getFormattedProgress(videoId: String): String? {
        // 1. Ưu tiên local progress
        val posMs = progressMap[videoId] ?: 0L
        if (posMs > 3000L) {
            return formatMs(posMs)
        }

        // 2. Fallback: remote exact progress (từ YouTube Web)
        val remoteExact = remoteExactProgressMap[videoId] ?: 0L
        if (remoteExact > 3000L) {
            return formatMs(remoteExact)
        }

        return null
    }

    private fun formatMs(posMs: Long): String {
        val totalSec = posMs / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun clearProgress(videoId: String) {
        progressMap.remove(videoId)
        remotePercentMap.remove(videoId)
        remoteExactProgressMap.remove(videoId)
        persist()
        _progressUpdated.value = System.currentTimeMillis()
    }

    private fun persist() {
        try {
            val json = JSONObject()
            for ((k, v) in progressMap) {
                json.put(k, v)
            }
            prefs?.edit()?.putString(KEY_PROGRESS_DATA, json.toString())?.apply()
        } catch (_: Exception) {}
    }
}

