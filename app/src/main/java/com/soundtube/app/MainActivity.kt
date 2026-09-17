package com.soundtube.app

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.soundtube.app.ui.components.FullscreenPlayerView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.soundtube.app.player.BackgroundPlayerManager
import com.soundtube.app.sync.YouTubeSyncManager
import com.soundtube.app.ui.MainViewModel
import com.soundtube.app.ui.components.FullPlayerSheet
import com.soundtube.app.ui.components.LoginSheet
import com.soundtube.app.ui.components.PlayerBar
import com.soundtube.app.ui.components.YouTubeBottomNav
import com.soundtube.app.ui.components.YouTubeNavTab
import com.soundtube.app.ui.components.YouTubeTopBar
import com.soundtube.app.ui.screens.HomeScreen
import com.soundtube.app.ui.screens.ShortsScreen
import com.soundtube.app.ui.screens.SubscriptionsScreen
import com.soundtube.app.ui.screens.YouScreen
import com.soundtube.app.ui.theme.DarkBackground
import com.soundtube.app.ui.theme.SoundTubeTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    // Trình yêu cầu cấp quyền thông báo cho Android 13+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Quyền thông báo đã được phản hồi
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()

        setContent {
            SoundTubeTheme {
                val selectedNavTab by viewModel.selectedNavTab.collectAsState()
                val selectedCategory by viewModel.selectedCategory.collectAsState()
                val isSearchActive by viewModel.isSearchActive.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val homeVideos by viewModel.homeVideos.collectAsState()
                val shortsVideos by viewModel.shortsVideos.collectAsState()
                val subscriptionsVideos by viewModel.subscriptionsVideos.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val isLoadingMore by viewModel.isLoadingMore.collectAsState()

                val currentSong by viewModel.currentSong.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val isBuffering by viewModel.isBuffering.collectAsState()
                val currentPositionMs by viewModel.currentPositionMs.collectAsState()
                val durationMs by viewModel.durationMs.collectAsState()
                val isPlayerSheetVisible by viewModel.isPlayerSheetVisible.collectAsState()
                val isVideoMode by viewModel.isVideoMode.collectAsState()
                val isFullscreen by viewModel.isFullscreen.collectAsState()
                val statusMessage by viewModel.statusMessage.collectAsState()

                val watchHistory by viewModel.watchHistory.collectAsState()
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                val userName by viewModel.userName.collectAsState()
                val userAvatar by viewModel.userAvatar.collectAsState()
                val isLoginSheetVisible by viewModel.isLoginSheetVisible.collectAsState()
                val volume by viewModel.volume.collectAsState()
                val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsState()
                val isSyncEnabled by viewModel.isSyncEnabled.collectAsState()
                val nextEpisode by viewModel.nextEpisode.collectAsState()
                val seriesEpisodes by viewModel.seriesEpisodes.collectAsState()
                val progressUpdated by viewModel.progressUpdated.collectAsState()

                // Nhận diện thiết bị là máy tính bảng (tablet) hoặc màn hình lớn (foldable)
                val isTablet = remember {
                    resources.configuration.smallestScreenWidthDp >= 600
                }

                // Điều khiển xoay ngang màn hình và ẩn thanh hệ thống khi xem Fullscreen
                LaunchedEffect(isFullscreen, isTablet) {
                    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                    if (isFullscreen) {
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            window.attributes.layoutInDisplayCutoutMode =
                                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                    } else {
                        // Trên Tablet: Cho phép xoay tự do theo hướng cầm máy (Landscape / Portrait), không ép dọc
                        // Trên Phone: Mặc định giữ ở chế độ xoay dọc (Portrait)
                        requestedOrientation = if (isTablet) {
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            window.attributes.layoutInDisplayCutoutMode =
                                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                        }
                    }
                }

                // Xử lý nút Back của Android
                BackHandler(enabled = isFullscreen || isLoginSheetVisible || isPlayerSheetVisible || isSearchActive || selectedNavTab != YouTubeNavTab.HOME) {
                    when {
                        isFullscreen -> viewModel.exitFullscreen()
                        isLoginSheetVisible -> viewModel.closeLoginSheet()
                        isPlayerSheetVisible -> viewModel.closePlayerSheet()
                        isSearchActive -> viewModel.closeSearch()
                        selectedNavTab != YouTubeNavTab.HOME -> viewModel.selectNavTab(YouTubeNavTab.HOME)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (!isFullscreen) Modifier.statusBarsPadding() else Modifier)
                    ) {
                        // 0a. Gắn WebView của BackgroundPlayerManager khi không ở chế độ xem video toàn màn hình
                        if (!isFullscreen && (!isPlayerSheetVisible || !isVideoMode)) {
                            AndroidView(
                                factory = { ctx ->
                                    BackgroundPlayerManager.getOrCreateWebView(ctx).apply {
                                        isClickable = false
                                        isFocusable = false
                                    }
                                },
                                modifier = Modifier.size(1.dp)
                            )
                        }

                        // 0b. Gắn WebView của YouTubeSyncManager vào View Hierarchy để tự động đồng bộ lịch sử xem với YouTube
                        AndroidView(
                            factory = { ctx ->
                                YouTubeSyncManager.getOrCreateWebView(ctx).apply {
                                    isClickable = false
                                    isFocusable = false
                                }
                            },
                            modifier = Modifier.size(1.dp)
                        )

                        // Giao diện chính của ứng dụng
                        Column(modifier = Modifier.fillMaxSize()) {
                            // 1. YouTube TopBar (Logo + Cast + Chuông thông báo badge 5 + Tìm kiếm + Avatar)
                            YouTubeTopBar(
                                onSearchClick = viewModel::openSearch,
                                onAvatarClick = { viewModel.selectNavTab(YouTubeNavTab.YOU) },
                                userAvatarUrl = userAvatar,
                                isLoggedIn = isLoggedIn
                            )

                            // 2. Nội dung hiển thị theo 5 tab Bottom Navigation
                            Box(modifier = Modifier.weight(1f)) {
                                when (selectedNavTab) {
                                    YouTubeNavTab.HOME -> {
                                        HomeScreen(
                                            selectedCategory = selectedCategory,
                                            searchQuery = searchQuery,
                                            isSearchActive = isSearchActive,
                                            searchResults = homeVideos,
                                            isLoading = isLoading,
                                            isLoadingMore = isLoadingMore,
                                            statusMessage = statusMessage,
                                            onCategorySelected = viewModel::selectCategory,
                                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                                            onSearch = viewModel::search,
                                            onCloseSearch = viewModel::closeSearch,
                                            onPlaySong = viewModel::playSong,
                                            onLoadMore = viewModel::loadMore
                                        )
                                    }
                                    YouTubeNavTab.SHORTS -> {
                                        ShortsScreen(
                                            shortsList = shortsVideos,
                                            isLoading = isLoading,
                                            onPlayShort = viewModel::playSong
                                        )
                                    }
                                    YouTubeNavTab.CREATE -> {
                                        // Tab tạo dẫn thẳng vào tìm kiếm / dán link nhanh
                                        HomeScreen(
                                            selectedCategory = selectedCategory,
                                            searchQuery = searchQuery,
                                            isSearchActive = true,
                                            searchResults = homeVideos,
                                            isLoading = isLoading,
                                            isLoadingMore = isLoadingMore,
                                            statusMessage = statusMessage,
                                            onCategorySelected = viewModel::selectCategory,
                                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                                            onSearch = viewModel::search,
                                            onCloseSearch = viewModel::closeSearch,
                                            onPlaySong = viewModel::playSong,
                                            onLoadMore = viewModel::loadMore
                                        )
                                    }
                                    YouTubeNavTab.SUBSCRIPTIONS -> {
                                        SubscriptionsScreen(
                                            videoList = subscriptionsVideos,
                                            isLoading = isLoading,
                                            onPlayVideo = viewModel::playSong
                                        )
                                    }
                                    YouTubeNavTab.YOU -> {
                                        YouScreen(
                                            watchHistory = watchHistory,
                                            isLoggedIn = isLoggedIn,
                                            userName = userName,
                                            userAvatar = userAvatar,
                                            isAutoplayEnabled = isAutoplayEnabled,
                                            isSyncEnabled = isSyncEnabled,
                                            onToggleAutoplay = viewModel::toggleAutoplay,
                                            onToggleSync = viewModel::toggleSync,
                                            onPlaySong = viewModel::playSong,
                                            onOpenLogin = viewModel::openLoginSheet,
                                            onLogout = viewModel::logout,
                                            onClearHistory = viewModel::clearHistory
                                        )
                                    }
                                }
                            }

                            // 3. Mini PlayerBar gắn trực tiếp ngay trên Bottom Navigation
                            PlayerBar(
                                song = currentSong,
                                isPlaying = isPlaying,
                                isBuffering = isBuffering,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onNext = viewModel::playNextSong,
                                onClose = viewModel::closeMiniPlayer,
                                onClick = viewModel::openPlayerSheet
                            )

                            // 4. Thanh điều hướng đáy 5 mục chuẩn YouTube (Trang chủ, Shorts, +, Kênh đăng ký, Bạn)
                            YouTubeBottomNav(
                                selectedTab = selectedNavTab,
                                onTabSelected = viewModel::selectNavTab,
                                userAvatarUrl = userAvatar
                            )
                        }

                        // 5. Trình phát toàn màn hình (Full Player Sheet)
                        FullPlayerSheet(
                            isVisible = isPlayerSheetVisible && !isFullscreen,
                            song = currentSong,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            volume = volume,
                            isAutoplayEnabled = isAutoplayEnabled,
                            isVideoMode = isVideoMode,
                            nextEpisode = nextEpisode,
                            seriesEpisodes = seriesEpisodes,
                            onClose = viewModel::closePlayerSheet,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onNext = viewModel::playNextSong,
                            onPrevious = viewModel::playPrevSong,
                            onToggleAutoplay = viewModel::toggleAutoplay,
                            onToggleVideoMode = viewModel::toggleVideoMode,
                            onEnterFullscreen = viewModel::enterFullscreen,
                            onVolumeChanged = viewModel::setVolume,
                            onSeekTo = viewModel::seekTo,
                            onSeekForward = viewModel::seekForward,
                            onSeekBackward = viewModel::seekBackward,
                            onPlaySong = viewModel::playSong,
                            onRestartFromBeginning = viewModel::restartSongFromBeginning
                        )

                        // 6. Màn hình đăng nhập YouTube (Login Sheet)
                        LoginSheet(
                            isVisible = isLoginSheetVisible,
                            onClose = viewModel::closeLoginSheet,
                            onLoginSuccess = viewModel::closeLoginSheet
                        )

                        // 7. Giao diện xem video toàn màn hình (Fullscreen Landscape View)
                        if (isFullscreen && isVideoMode && currentSong != null) {
                            FullscreenPlayerView(
                                song = currentSong!!,
                                isPlaying = isPlaying,
                                isBuffering = isBuffering,
                                currentPositionMs = currentPositionMs,
                                durationMs = durationMs,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSeekTo = viewModel::seekTo,
                                onSeekForward = viewModel::seekForward,
                                onSeekBackward = viewModel::seekBackward,
                                onNext = viewModel::playNextSong,
                                onPrevious = viewModel::playPrevSong,
                                onExitFullscreen = viewModel::exitFullscreen
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
