package com.soundtube.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

object PlaybackProgressManager {
    private const val PREFS_NAME = "soundtube_playback_progress"
    private const val KEY_PROGRESS_DATA = "progress_data"

    private var prefs: SharedPreferences? = null
    private val progressMap = mutableMapOf<String, Long>()

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
     * Lưu vị trí phát của video tính bằng mili-giây
     */
    fun saveProgress(videoId: String, positionMs: Long, durationMs: Long) {
        if (videoId.isBlank() || positionMs < 0L) return

        // Nếu đã nghe gần hết (trên 95% thời lượng), xem như đã hoàn thành -> xóa tiến trình để lần sau nghe lại từ đầu
        if (durationMs > 0 && positionMs >= durationMs * 0.95f) {
            progressMap.remove(videoId)
            persist()
            _progressUpdated.value = System.currentTimeMillis()
            return
        }

        // Chỉ lưu khi đã nghe được hơn 3 giây
        if (positionMs > 3000L) {
            progressMap[videoId] = positionMs
            persist()
            _progressUpdated.value = System.currentTimeMillis()
        }
    }

    /**
     * Lấy vị trí đã dừng của video (mili-giây), trả về 0L nếu chưa từng nghe hoặc đã nghe xong
     */
    fun getProgress(videoId: String): Long {
        return progressMap[videoId] ?: 0L
    }

    /**
     * Lấy tỉ lệ tiến trình đã nghe từ 0.0f đến 1.0f (phục vụ hiển thị thanh đỏ trên thumbnail)
     */
    fun getProgressRatio(videoId: String, durationSeconds: Long): Float {
        val posMs = getProgress(videoId)
        val durationMs = durationSeconds * 1000L
        if (durationMs <= 0 || posMs <= 0) return 0f
        return (posMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Định dạng chuỗi phút dừng (ví dụ: "18:35")
     */
    fun getFormattedProgress(videoId: String): String? {
        val posMs = getProgress(videoId)
        if (posMs <= 3000L) return null
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
