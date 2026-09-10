package com.soundtube.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundtube.app.data.AuthManager
import com.soundtube.app.data.EpisodeManager
import com.soundtube.app.data.HistoryManager
import com.soundtube.app.data.PlaybackProgressManager
import com.soundtube.app.data.YouTubeExtractor
import com.soundtube.app.data.model.Song
import com.soundtube.app.player.BackgroundPlayerManager
import com.soundtube.app.service.PlaybackService
import com.soundtube.app.sync.YouTubeSyncManager
import com.soundtube.app.ui.components.YouTubeNavTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // 1. Tab điều hướng đáy (Bottom Navigation Tab): HOME, SHORTS, CREATE, SUBSCRIPTIONS, YOU
    private val _selectedNavTab = MutableStateFlow(YouTubeNavTab.HOME)
    val selectedNavTab: StateFlow<YouTubeNavTab> = _selectedNavTab.asStateFlow()

    // 2. Thẻ danh mục Trang chủ (Tất cả, Âm nhạc, Danh sách kết hợp, ...)
    private val _selectedCategory = MutableStateFlow("Tất cả")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // 3. Trạng thái ô tìm kiếm
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Danh sách video trên Trang chủ
    private val _homeVideos = MutableStateFlow<List<Song>>(emptyList())
    val homeVideos: StateFlow<List<Song>> = _homeVideos.asStateFlow()

    // Danh sách video Shorts
    private val _shortsVideos = MutableStateFlow<List<Song>>(emptyList())
    val shortsVideos: StateFlow<List<Song>> = _shortsVideos.asStateFlow()

    // Danh sách video Kênh đăng ký
    private val _subscriptionsVideos = MutableStateFlow<List<Song>>(emptyList())
    val subscriptionsVideos: StateFlow<List<Song>> = _subscriptionsVideos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Trạng thái Tải thêm (Load More)
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Phân trang & Xoay vòng bảng tin
    private var currentContinuationToken: String? = null
    private var currentPoolIndex = 0
    private val categoryRotationIndices = mutableMapOf<String, Int>()
    private var searchContinuationToken: String? = null
    private val recentShownVideoIds = LinkedHashSet<String>()

    // Hàng đợi phát nhạc (Play Queue)
    private val _playQueue = MutableStateFlow<List<Song>>(emptyList())
    val playQueue: StateFlow<List<Song>> = _playQueue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Tự động phát tiếp (Auto-play)
    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    // Quản lý Tập kế tiếp & Danh sách các tập của bộ truyện
    private val _nextEpisode = MutableStateFlow<Song?>(null)
    val nextEpisode: StateFlow<Song?> = _nextEpisode.asStateFlow()

    private val _seriesEpisodes = MutableStateFlow<List<Song>>(emptyList())
    val seriesEpisodes: StateFlow<List<Song>> = _seriesEpisodes.asStateFlow()

    private val _isLoadingEpisodes = MutableStateFlow(false)
    val isLoadingEpisodes: StateFlow<Boolean> = _isLoadingEpisodes.asStateFlow()

    // Lắng nghe cập nhật tiến trình để kích hoạt recomposition giao diện
    val progressUpdated: StateFlow<Long> = PlaybackProgressManager.progressUpdated

    // Âm lượng (0.0f .. 1.0f)
    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Trạng thái phát nhạc từ BackgroundPlayerManager
    val currentSong: StateFlow<Song?> = BackgroundPlayerManager.currentSong
    val isPlaying: StateFlow<Boolean> = BackgroundPlayerManager.isPlaying
    val isBuffering: StateFlow<Boolean> = BackgroundPlayerManager.isBuffering
    val currentPositionMs: StateFlow<Long> = BackgroundPlayerManager.currentPositionMs
    val durationMs: StateFlow<Long> = BackgroundPlayerManager.durationMs

    private val _isPlayerSheetVisible = MutableStateFlow(false)
    val isPlayerSheetVisible: StateFlow<Boolean> = _isPlayerSheetVisible.asStateFlow()

    // Chế độ xem Video (true: hiện video trực tiếp, false: hiện ảnh bìa album tiết kiệm pin)
    private val _isVideoMode = MutableStateFlow(true)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode.asStateFlow()

    // Chế độ toàn màn hình (xoay ngang)
    private val _isFullscreen = MutableStateFlow(false)
    val isFullscreen: StateFlow<Boolean> = _isFullscreen.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Quản lý lịch sử và tài khoản
    val watchHistory: StateFlow<List<Song>> = HistoryManager.history
    val isLoggedIn: StateFlow<Boolean> = AuthManager.isLoggedIn
    val userName: StateFlow<String?> = AuthManager.userName
    val userAvatar: StateFlow<String?> = AuthManager.userAvatar

    private val _isLoginSheetVisible = MutableStateFlow(false)
    val isLoginSheetVisible: StateFlow<Boolean> = _isLoginSheetVisible.asStateFlow()

    // Đồng bộ YouTube chính thức
    val isSyncEnabled: StateFlow<Boolean> = YouTubeSyncManager.syncEnabled

    init {
        // Khởi tạo Player, History, Auth, PlaybackProgress
        BackgroundPlayerManager.initialize(application)
        HistoryManager.initialize(application)
        AuthManager.initialize(application)
        PlaybackProgressManager.initialize(application)

        // Đọc mức âm lượng hệ thống ban đầu
        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            if (maxVol > 0) {
                _volume.value = (curVol / maxVol).coerceIn(0f, 1f)
            }
        } catch (_: Exception) {}

        // Kết nối sự kiện kết thúc bài hát để tự động phát tập tiếp theo hoặc bài kế tiếp (Auto-play)
        BackgroundPlayerManager.onSongEndedListener = {
            if (_isAutoplayEnabled.value) {
                viewModelScope.launch {
                    val nextEp = _nextEpisode.value
                    if (nextEp != null) {
                        playSong(nextEp, _seriesEpisodes.value.ifEmpty { _playQueue.value })
                    } else {
                        playNextSong()
                    }
                }
            }
        }

        // Kết nối nút Next & Prev từ notification màn hình khóa và Bluetooth
        PlaybackService.onNextListener = {
            viewModelScope.launch {
                playNextSong()
            }
        }
        PlaybackService.onPrevListener = {
            viewModelScope.launch {
                playPrevSong()
            }
        }

        // Tải bảng tin Trang chủ mặc định
        loadCategoryFeed("Tất cả")

        // Lắng nghe trạng thái đăng nhập để tự động tải lịch sử xem từ YouTube
        viewModelScope.launch {
            AuthManager.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    loadWatchHistory()
                }
            }
        }
    }

    fun selectNavTab(tab: YouTubeNavTab) {
        if (tab == YouTubeNavTab.CREATE) {
            // Khi bấm nút Tạo (+), mở ô tìm kiếm nhanh hoặc dán link
            openSearch()
            return
        }

        val isReselectingHome = (_selectedNavTab.value == YouTubeNavTab.HOME && tab == YouTubeNavTab.HOME)
        _selectedNavTab.value = tab

        if (isReselectingHome) {
            // Bấm lại tab Trang chủ khi đang ở Trang chủ -> Xoay vòng làm mới bảng tin
            loadCategoryFeed(_selectedCategory.value, forceRotate = true)
            return
        }

        when (tab) {
            YouTubeNavTab.SHORTS -> {
                if (_shortsVideos.value.isEmpty()) {
                    loadShorts()
                }
            }
            YouTubeNavTab.SUBSCRIPTIONS -> {
                if (_subscriptionsVideos.value.isEmpty()) {
                    loadSubscriptions()
                }
            }
            YouTubeNavTab.YOU -> {
                loadWatchHistory()
            }
            else -> {}
        }
    }

    fun selectCategory(category: String) {
        val isSameCategory = _selectedCategory.value == category
        _selectedCategory.value = category
        if (isSameCategory) {
            // Bấm vào category đang chọn -> Xoay vòng làm mới ngay lập tức
            loadCategoryFeed(category, forceRotate = true)
        } else {
            loadCategoryFeed(category, forceRotate = false)
        }
    }

    fun openSearch() {
        _isSearchActive.value = true
    }

    fun closeSearch() {
        _isSearchActive.value = false
        searchContinuationToken = null
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun search(query: String = _searchQuery.value) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null
            searchContinuationToken = null
            try {
                val pageResult = YouTubeExtractor.searchPage(query)
                _homeVideos.value = pageResult.videos
                searchContinuationToken = pageResult.continuationToken
                if (pageResult.videos.isEmpty()) {
                    _statusMessage.value = "Không tìm thấy kết quả"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Lỗi kết nối: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCategoryFeed(category: String, forceRotate: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = null

            // Lưu các video hiện tại vào cache loại trừ để không bao giờ bị lặp lại
            _homeVideos.value.forEach { recentShownVideoIds.add(it.id) }
            if (recentShownVideoIds.size > 300) {
                val toRemove = recentShownVideoIds.take(100).toSet()
                recentShownVideoIds.removeAll(toRemove)
            }

            if (forceRotate) {
                val nextIdx = (categoryRotationIndices[category] ?: 0) + 1
                categoryRotationIndices[category] = nextIdx
            }
            currentPoolIndex = categoryRotationIndices[category] ?: 0
            currentContinuationToken = null

            try {
                val pageResult = YouTubeExtractor.getFeedForCategory(
                    category = category,
                    userHistory = watchHistory.value,
                    continuationToken = null,
                    poolIndex = currentPoolIndex,
                    excludeIds = recentShownVideoIds
                )
                _homeVideos.value = pageResult.videos
                currentContinuationToken = pageResult.continuationToken
                pageResult.videos.forEach { recentShownVideoIds.add(it.id) }

                if (pageResult.videos.isEmpty()) {
                    _statusMessage.value = "Chưa có video cho danh mục này"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Lỗi tải bảng tin: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cuộn tải thêm video vô tận (Infinite Load More)
     */
    fun loadMore() {
        if (_isLoading.value || _isLoadingMore.value) return

        if (_isSearchActive.value) {
            loadMoreSearch()
            return
        }

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val tokenToUse = currentContinuationToken
                if (tokenToUse == null) {
                    currentPoolIndex++
                }

                val feedResult = YouTubeExtractor.getFeedForCategory(
                    category = _selectedCategory.value,
                    userHistory = watchHistory.value,
                    continuationToken = tokenToUse,
                    poolIndex = currentPoolIndex,
                    excludeIds = recentShownVideoIds
                )

                if (feedResult.videos.isNotEmpty()) {
                    feedResult.videos.forEach { recentShownVideoIds.add(it.id) }
                    val updatedList = (_homeVideos.value + feedResult.videos).distinctBy { it.id }
                    _homeVideos.value = updatedList
                    currentContinuationToken = feedResult.continuationToken
                } else if (tokenToUse != null) {
                    // Fallback sang query pool nếu continuation token hết hạn
                    currentContinuationToken = null
                    currentPoolIndex++
                    val fallbackResult = YouTubeExtractor.getFeedForCategory(
                        category = _selectedCategory.value,
                        userHistory = watchHistory.value,
                        continuationToken = null,
                        poolIndex = currentPoolIndex,
                        excludeIds = recentShownVideoIds
                    )
                    if (fallbackResult.videos.isNotEmpty()) {
                        fallbackResult.videos.forEach { recentShownVideoIds.add(it.id) }
                        val updatedList = (_homeVideos.value + fallbackResult.videos).distinctBy { it.id }
                        _homeVideos.value = updatedList
                        currentContinuationToken = fallbackResult.continuationToken
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Lỗi loadMore: ${e.message}")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun loadMoreSearch() {
        val token = searchContinuationToken ?: return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val pageResult = YouTubeExtractor.searchPage(_searchQuery.value, token)
                if (pageResult.videos.isNotEmpty()) {
                    val updatedList = (_homeVideos.value + pageResult.videos).distinctBy { it.id }
                    _homeVideos.value = updatedList
                    searchContinuationToken = pageResult.continuationToken
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Lỗi loadMoreSearch: ${e.message}")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun loadShorts() {
        viewModelScope.launch {
            try {
                val shorts = YouTubeExtractor.getShorts()
                _shortsVideos.value = shorts
            } catch (_: Exception) {}
        }
    }

    private fun loadSubscriptions() {
        viewModelScope.launch {
            try {
                val subs = YouTubeExtractor.search("kênh đăng ký nhạc việt nam 2026")
                _subscriptionsVideos.value = subs
            } catch (_: Exception) {}
        }
    }

    /**
     * Bắt đầu phát bài hát và cập nhật hàng đợi.
     * Tự động nhớ và tiếp tục phát từ phút/giây đã dừng lần trước nếu có.
     */
    fun playSong(song: Song, queue: List<Song> = _homeVideos.value, fromBeginning: Boolean = false, openSheet: Boolean = true) {
        val currentList = if (queue.isNotEmpty()) queue else listOf(song)
        _playQueue.value = currentList
        val index = currentList.indexOfFirst { it.id == song.id }
        _currentIndex.value = if (index >= 0) index else 0

        // Lưu vào lịch sử đã xem
        HistoryManager.addSong(song)

        val app = getApplication<Application>()
        val serviceIntent = Intent(app, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(serviceIntent)
        } else {
            app.startService(serviceIntent)
        }

        // Mở màn hình phát toàn màn hình khi người dùng chọn video
        if (openSheet) {
            _isPlayerSheetVisible.value = true
        }

        // Lấy vị trí đã nghe dở lần trước (nếu không yêu cầu nghe lại từ đầu)
        val startPosMs = if (fromBeginning) 0L else PlaybackProgressManager.getProgress(song.id)
        BackgroundPlayerManager.playSong(song, startPosMs)

        // Tải đề xuất tập kế tiếp và danh sách các tập của bộ truyện này
        loadEpisodeRecommendations(song)
    }

    private fun loadEpisodeRecommendations(song: Song) {
        viewModelScope.launch {
            _isLoadingEpisodes.value = true
            try {
                val next = EpisodeManager.findNextEpisode(song)
                _nextEpisode.value = next
                val series = EpisodeManager.findSeriesEpisodes(song)
                _seriesEpisodes.value = series
            } catch (_: Exception) {
                _nextEpisode.value = null
                _seriesEpisodes.value = emptyList()
            } finally {
                _isLoadingEpisodes.value = false
            }
        }
    }

    fun restartSongFromBeginning() {
        val song = currentSong.value ?: return
        PlaybackProgressManager.clearProgress(song.id)
        playSong(song, _playQueue.value, fromBeginning = true)
    }

    fun playNextSong() {
        // Nếu đây là truyện/audiobook và có tập tiếp theo cùng kênh, ưu tiên phát tập tiếp theo
        val nextEp = _nextEpisode.value
        if (nextEp != null) {
            playSong(nextEp, _seriesEpisodes.value.ifEmpty { _playQueue.value })
            return
        }

        val queue = _playQueue.value
        if (queue.isEmpty()) return

        val nextIndex = _currentIndex.value + 1
        if (nextIndex < queue.size) {
            val nextSong = queue[nextIndex]
            playSong(nextSong, queue)
        } else {
            val firstSong = queue[0]
            playSong(firstSong, queue)
        }
    }

    fun playPrevSong() {
        if (currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val queue = _playQueue.value
        if (queue.isEmpty()) return

        val prevIndex = _currentIndex.value - 1
        if (prevIndex >= 0) {
            val prevSong = queue[prevIndex]
            playSong(prevSong, queue)
        } else {
            seekTo(0L)
        }
    }

    fun toggleAutoplay() {
        _isAutoplayEnabled.value = !_isAutoplayEnabled.value
    }

    fun toggleSync() {
        YouTubeSyncManager.setSyncEnabled(!YouTubeSyncManager.syncEnabled.value)
    }

    fun setVolume(newVolume: Float) {
        val clamped = newVolume.coerceIn(0f, 1f)
        _volume.value = clamped

        try {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetLevel = (clamped * maxVol).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetLevel, 0)
        } catch (_: Exception) {}

        BackgroundPlayerManager.setVolume((clamped * 100).toInt())
    }

    fun togglePlayPause() {
        BackgroundPlayerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        BackgroundPlayerManager.seekTo(positionMs)
    }

    fun seekForward() {
        BackgroundPlayerManager.seekForward(10000L)
    }

    fun seekBackward() {
        BackgroundPlayerManager.seekBackward(10000L)
    }

    fun openPlayerSheet() {
        _isPlayerSheetVisible.value = true
    }

    fun closePlayerSheet() {
        _isPlayerSheetVisible.value = false
    }

    fun toggleVideoMode() {
        _isVideoMode.value = !_isVideoMode.value
        if (!_isVideoMode.value) {
            _isFullscreen.value = false
        }
    }

    fun setVideoMode(enabled: Boolean) {
        _isVideoMode.value = enabled
        if (!enabled) {
            _isFullscreen.value = false
        }
    }

    fun enterFullscreen() {
        if (_isVideoMode.value && currentSong.value != null) {
            _isFullscreen.value = true
        }
    }

    fun exitFullscreen() {
        _isFullscreen.value = false
    }

    fun toggleFullscreen() {
        if (_isFullscreen.value) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    fun closeMiniPlayer() {
        _isFullscreen.value = false
        _isPlayerSheetVisible.value = false
        BackgroundPlayerManager.stop()
        val app = getApplication<Application>()
        val stopIntent = Intent(app, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_STOP
        }
        app.startService(stopIntent)
    }

    // Quản lý màn hình đăng nhập
    fun openLoginSheet() {
        _isLoginSheetVisible.value = true
    }

    fun closeLoginSheet() {
        _isLoginSheetVisible.value = false
        AuthManager.checkYouTubeCookies()
    }

    fun logout() {
        AuthManager.logout()
    }

    fun clearHistory() {
        HistoryManager.clearHistory()
    }

    /**
     * Tải và đồng bộ danh sách lịch sử xem từ YouTube chính thức
     */
    fun loadWatchHistory() {
        viewModelScope.launch {
            try {
                if (AuthManager.isLoggedIn.value) {
                    val remoteHistory = YouTubeExtractor.fetchYouTubeWatchHistory()
                    if (remoteHistory.isNotEmpty()) {
                        HistoryManager.syncWithYouTubeHistory(remoteHistory)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Lỗi tải lịch sử xem YouTube: ${e.message}")
            }
        }
    }
}
