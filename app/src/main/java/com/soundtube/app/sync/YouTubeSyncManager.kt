package com.soundtube.app.sync

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.soundtube.app.data.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * YouTubeSyncManager - Quản lý WebView ngầm để đồng bộ lịch sử xem & watch progress trực tiếp với YouTube chính thức.
 *
 * Cơ chế hoạt động:
 * - Dùng WebView gắn trong View Hierarchy (kích thước 1x1 dp, tắt tiếng) load trực tiếp trang xem chính thức:
 *   https://m.youtube.com/watch?v={videoId}
 * - Sử dụng chung CookieManager với tài khoản Google đã đăng nhập -> YouTube nhận diện phiên xem First-Party (el=detailpage)
 * - Tự động phát ngầm (muted=true) để YouTube gửi telemetry /api/stats/playback và /api/stats/watchtime
 * - Nhờ đó, video được lưu chính xác vào YouTube Watch History (youtube.com/feed/history),
 *   hiển thị thanh đỏ tiến trình xem và cập nhật thuật toán gợi ý của YouTube.
 */
@SuppressLint("StaticFieldLeak", "SetJavaScriptEnabled")
object YouTubeSyncManager {
    private const val TAG = "YouTubeSyncManager"

    private var syncWebView: SyncWebView? = null
    private var currentSyncVideoId: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var syncProgressRunnable: Runnable? = null

    // Sync interval: 10 giây
    private const val SYNC_INTERVAL_MS = 10_000L

    private var lastKnownPositionSec: Double = 0.0
    private var lastSyncedPositionSec: Double = -1.0

    // Trạng thái đồng bộ để UI hiển thị
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncEnabled = MutableStateFlow(true)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    class SyncWebView(context: Context) : WebView(context) {
        var isBackgroundPlaybackEnabled: Boolean = true

        override fun onWindowVisibilityChanged(visibility: Int) {
            if (isBackgroundPlaybackEnabled && (visibility == View.GONE || visibility == View.INVISIBLE)) {
                // Giữ cho Chromium tiếp tục chạy ngầm khi màn hình tắt hoặc app chuyển xuống nền
                return
            }
            super.onWindowVisibilityChanged(visibility)
        }
    }

    /**
     * Khởi tạo Sync WebView. Gọi trong Application.onCreate().
     */
    fun initialize(context: Context) {
        if (syncWebView != null) return

        if (Looper.myLooper() == Looper.getMainLooper()) {
            setupSyncWebView(context)
        } else {
            mainHandler.post { setupSyncWebView(context) }
        }
    }

    /**
     * Lấy hoặc tạo WebView để gắn vào Compose View Hierarchy (MainActivity)
     */
    fun getOrCreateWebView(context: Context): WebView {
        if (syncWebView == null) {
            setupSyncWebView(context)
        }
        val wv = syncWebView!!
        (wv.parent as? ViewGroup)?.removeView(wv)
        return wv
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupSyncWebView(context: Context) {
        if (syncWebView != null) return
        try {
            val wv = SyncWebView(context.applicationContext)

            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
            }

            // Bật chia sẻ cookies với Google / YouTube
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(wv, true)

            wv.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url?.toString() ?: ""
                    // Ghi log kiểm tra các lượt gửi telemetry lịch sử xem
                    if (url.contains("/api/stats/playback") || url.contains("/api/stats/watchtime") ||
                        url.contains("/stats/playback") || url.contains("/stats/watchtime")) {
                        Log.i(TAG, ">>> YOUTUBE TELEMETRY DETECTED (Watch History Logged): $url")
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Sync WebView page loaded: $url")

                    val startSec = lastKnownPositionSec
                    // Inject JavaScript để:
                    // 1. Tắt tiếng hoàn toàn thẻ video/audio để không phát trùng âm thanh
                    // 2. Seek tới vị trí startSec nếu > 3 giây (để báo cáo đúng thời điểm dừng)
                    // 3. Ép video tự phát (muted autoplay) để kích hoạt API watchtime của YouTube
                    // 4. Đóng các popup đề xuất ứng dụng/cookie consent
                    view?.evaluateJavascript("""
                        (function() {
                            var targetSec = $startSec;
                            var hasSeeked = false;

                            function enforceSilentPlayback() {
                                try {
                                    var videos = document.querySelectorAll('video');
                                    for (var i = 0; i < videos.length; i++) {
                                        var v = videos[i];
                                        v.muted = true;
                                        v.volume = 0;
                                        v.setAttribute('muted', 'true');
                                        if (!hasSeeked && targetSec > 3.0) {
                                            v.currentTime = targetSec;
                                            hasSeeked = true;
                                        }
                                        if (v.paused) {
                                            v.play().catch(function(e) {});
                                        }
                                    }
                                } catch(e) {}
                            }

                            enforceSilentPlayback();
                            setInterval(enforceSilentPlayback, 1200);

                            // Tự động đóng các banner hoặc dialog nếu có
                            try {
                                var dismissBtn = document.querySelector('button[aria-label*="Không"], button[aria-label*="No thanks"], button[aria-label*="Dismiss"], .eom-dialog button');
                                if (dismissBtn) dismissBtn.click();
                            } catch(e) {}

                            // Tối ưu hiệu năng: ẩn các thành phần UI không cần thiết
                            try {
                                var style = document.createElement('style');
                                style.innerHTML = 'header, ytm-mobile-topbar-renderer, ytm-item-section-renderer, ytm-comments-entry-point-header-renderer, .related-items, ytm-pivot-bar-renderer { display: none !important; } body { background: black !important; }';
                                document.head.appendChild(style);
                            } catch(e) {}
                        })();
                    """.trimIndent(), null)
                }
            }

            wv.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    val msg = consoleMessage?.message() ?: ""
                    if (msg.contains("SYNC_")) {
                        Log.d(TAG, "Sync Console: $msg")
                    }
                    return true
                }
            }

            // Kích thước tối thiểu nhưng vẫn Visible để Chromium xử lý video pipeline bình thường
            wv.visibility = View.VISIBLE
            wv.layoutParams = ViewGroup.LayoutParams(1, 1)

            syncWebView = wv
            Log.d(TAG, "Sync WebView initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init Sync WebView: ${e.message}", e)
        }
    }

    /**
     * Kiểm tra user đã đăng nhập YouTube trong WebView chưa.
     */
    fun isYouTubeLoggedIn(): Boolean {
        return try {
            val cookies = CookieManager.getInstance().getCookie("https://www.youtube.com") ?: ""
            cookies.contains("LOGIN_INFO") || cookies.contains("SAPISID") || cookies.contains("SSID")
        } catch (e: Exception) {
            false
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        _syncEnabled.value = enabled
        if (!enabled) {
            stopSync()
        }
    }

    /**
     * Bắt đầu đồng bộ video đang phát lên tài khoản YouTube chính thức.
     * Load https://m.youtube.com/watch?v={videoId} kèm cookie tài khoản đã đăng nhập.
     */
    fun startSync(videoId: String, startPositionSec: Double = 0.0) {
        if (!_syncEnabled.value) return
        if (!AuthManager.isLoggedIn.value && !isYouTubeLoggedIn()) {
            Log.d(TAG, "Skip sync: user not logged in to YouTube")
            return
        }

        mainHandler.post {
            val wv = syncWebView ?: return@post

            if (currentSyncVideoId == videoId) {
                // Đã đang ở video này -> Chỉ cần seek tới vị trí
                seekSyncVideo(startPositionSec)
                resumeSync()
                return@post
            }

            currentSyncVideoId = videoId
            _isSyncing.value = true
            lastKnownPositionSec = startPositionSec
            lastSyncedPositionSec = startPositionSec

            val startSec = startPositionSec.toInt()
            val watchUrl = if (startSec > 5) {
                "https://m.youtube.com/watch?v=$videoId&t=${startSec}s"
            } else {
                "https://m.youtube.com/watch?v=$videoId"
            }

            Log.d(TAG, "Starting first-party YouTube sync for video: $videoId at URL: $watchUrl")
            wv.loadUrl(watchUrl)

            startSyncPositionLoop()
        }
    }

    /**
     * Cập nhật vị trí phát từ Player chính sang Sync WebView.
     */
    fun syncPosition(positionSec: Double) {
        if (!_isSyncing.value) return
        lastKnownPositionSec = positionSec
    }

    private fun seekSyncVideo(positionSec: Double) {
        val pos = positionSec
        syncWebView?.evaluateJavascript("""
            (function() {
                try {
                    var v = document.querySelector('video');
                    if (v) {
                        v.currentTime = $pos;
                        v.muted = true;
                        v.volume = 0;
                        if (v.paused) v.play().catch(function(e) {});
                    }
                } catch(e) {}
            })();
        """.trimIndent(), null)
        lastSyncedPositionSec = pos
    }

    private fun startSyncPositionLoop() {
        stopSyncPositionLoop()

        syncProgressRunnable = object : Runnable {
            override fun run() {
                if (!_isSyncing.value || syncWebView == null) return

                val pos = lastKnownPositionSec
                // Chỉ seek nếu vị trí sai lệch đáng kể (> 8 giây)
                if (Math.abs(pos - lastSyncedPositionSec) > 8.0) {
                    seekSyncVideo(pos)
                    Log.d(TAG, "Synced position to ${pos.toInt()}s")
                }

                mainHandler.postDelayed(this, SYNC_INTERVAL_MS)
            }
        }
        mainHandler.postDelayed(syncProgressRunnable!!, SYNC_INTERVAL_MS)
    }

    private fun stopSyncPositionLoop() {
        syncProgressRunnable?.let { mainHandler.removeCallbacks(it) }
        syncProgressRunnable = null
    }

    fun pauseSync() {
        if (!_isSyncing.value) return
        mainHandler.post {
            syncWebView?.evaluateJavascript("""
                (function() {
                    try {
                        var v = document.querySelector('video');
                        if (v) v.pause();
                    } catch(e) {}
                })();
            """.trimIndent(), null)
        }
        stopSyncPositionLoop()
        Log.d(TAG, "Sync paused")
    }

    fun resumeSync() {
        if (!_syncEnabled.value) return
        if (!AuthManager.isLoggedIn.value && !isYouTubeLoggedIn()) return
        if (currentSyncVideoId == null) return

        _isSyncing.value = true
        mainHandler.post {
            syncWebView?.evaluateJavascript("""
                (function() {
                    try {
                        var v = document.querySelector('video');
                        if (v) {
                            v.muted = true;
                            v.volume = 0;
                            v.play().catch(function(e) {});
                        }
                    } catch(e) {}
                })();
            """.trimIndent(), null)
        }
        startSyncPositionLoop()
        Log.d(TAG, "Sync resumed")
    }

    fun stopSync() {
        _isSyncing.value = false
        currentSyncVideoId = null
        lastKnownPositionSec = 0.0
        lastSyncedPositionSec = -1.0
        stopSyncPositionLoop()

        mainHandler.post {
            syncWebView?.evaluateJavascript("""
                (function() {
                    try {
                        var v = document.querySelector('video');
                        if (v) v.pause();
                    } catch(e) {}
                })();
            """.trimIndent(), null)
            syncWebView?.loadUrl("about:blank")
        }
        Log.d(TAG, "Sync stopped")
    }

    fun release() {
        stopSync()
        mainHandler.post {
            syncWebView?.destroy()
            syncWebView = null
        }
    }
}
