package com.soundtube.app.data

import android.content.Context
import android.content.SharedPreferences
import com.soundtube.app.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object HistoryManager {
    private const val PREFS_NAME = "soundtube_history_prefs"
    private const val KEY_HISTORY = "watch_history"
    private const val MAX_HISTORY_ITEMS = 100

    private var prefs: SharedPreferences? = null
    private val _history = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history.asStateFlow()

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadHistory()
        }
    }

    private fun loadHistory() {
        val jsonStr = prefs?.getString(KEY_HISTORY, null) ?: return
        try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Song>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        uploaderName = obj.optString("uploaderName", "YouTube"),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        thumbnailUrl = obj.optString("thumbnailUrl", "")
                    )
                )
            }
            _history.value = list
        } catch (e: Exception) {
            _history.value = emptyList()
        }
    }

    fun addSong(song: Song) {
        val currentList = _history.value.toMutableList()
        // Xóa mục cũ nếu đã tồn tại để đưa lên đầu danh sách
        currentList.removeAll { it.id == song.id }
        currentList.add(0, song)

        if (currentList.size > MAX_HISTORY_ITEMS) {
            currentList.subList(MAX_HISTORY_ITEMS, currentList.size).clear()
        }

        _history.value = currentList
        saveHistory(currentList)
    }

    /**
     * Đồng bộ lịch sử xem từ YouTube chính thức vào SoundTube
     * Ưu tiên danh sách remoteSongs ở đầu (mới nhất từ tài khoản YouTube thực tế)
     */
    fun syncWithYouTubeHistory(remoteSongs: List<Song>) {
        if (remoteSongs.isEmpty()) return
        val remoteIds = remoteSongs.map { it.id }.toSet()
        val localOnly = _history.value.filter { it.id !in remoteIds }
        val merged = (remoteSongs + localOnly).take(MAX_HISTORY_ITEMS)
        _history.value = merged
        saveHistory(merged)
    }

    fun removeSong(songId: String) {
        val currentList = _history.value.toMutableList()
        currentList.removeAll { it.id == songId }
        _history.value = currentList
        saveHistory(currentList)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs?.edit()?.remove(KEY_HISTORY)?.apply()
    }

    private fun saveHistory(list: List<Song>) {
        try {
            val jsonArray = JSONArray()
            for (song in list) {
                val obj = JSONObject().apply {
                    put("id", song.id)
                    put("title", song.title)
                    put("uploaderName", song.uploaderName)
                    put("durationSeconds", song.durationSeconds)
                    put("thumbnailUrl", song.thumbnailUrl)
                }
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString(KEY_HISTORY, jsonArray.toString())?.apply()
        } catch (_: Exception) {}
    }
}
