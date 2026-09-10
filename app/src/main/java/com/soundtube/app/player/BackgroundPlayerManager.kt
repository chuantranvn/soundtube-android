package com.soundtube.app.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.soundtube.app.data.PlaybackProgressManager
import com.soundtube.app.data.model.Song
import com.soundtube.app.sync.YouTubeSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("StaticFieldLeak", "SetJavaScriptEnabled")
object BackgroundPlayerManager {
    private const val TAG = "BackgroundPlayer"

    private var webView: BackgroundWebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isPlayerReady = false
    private var pendingSongToPlay: Song? = null
    private var pendingStartPositionMs: Long = 0L

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // Listener để PlaybackService cập nhật Notification ngoài màn hình khóa
    var onStateChangedListener: ((isPlaying: Boolean, song: Song?) -> Unit)? = null

    // Listener khi bài hát kết thúc để tự động phát bài kế tiếp (Auto-play)
    var onSongEndedListener: (() -> Unit)? = null

    class BackgroundWebView(context: Context) : WebView(context) {
        var isBackgroundPlaybackEnabled: Boolean = true

        override fun onWindowVisibilityChanged(visibility: Int) {
            if (isBackgroundPlaybackEnabled && (visibility == View.GONE || visibility == View.INVISIBLE)) {
                // Chặn Chromium tạm dừng audio/render khi tắt màn hình hoặc chuyển ứng dụng ngầm
                return
            }
            super.onWindowVisibilityChanged(visibility)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)

            val finalWidth = if (widthMode != MeasureSpec.UNSPECIFIED && widthSize > 0) widthSize else 0
            val finalHeight = if (heightMode != MeasureSpec.UNSPECIFIED && heightSize > 0) heightSize else 0

            if (finalWidth > 0 && finalHeight > 0) {
                setMeasuredDimension(finalWidth, finalHeight)
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            super.onSizeChanged(w, h, ow, oh)
            if (w > 0 && h > 0) {
                post {
                    evaluateJavascript("if (window.resizePlayer) { window.resizePlayer(); }", null)
                }
            }
        }
    }

    fun triggerResize() {
        mainHandler.post {
            webView?.evaluateJavascript("if (window.resizePlayer) { window.resizePlayer(); }", null)
        }
    }

    fun initialize(context: Context) {
        if (webView != null) return

        if (Looper.myLooper() == Looper.getMainLooper()) {
            setupWebViewInstance(context)
        } else {
            mainHandler.post {
                setupWebViewInstance(context)
            }
        }
    }

    fun getOrCreateWebView(context: Context): WebView {
        if (webView == null) {
            setupWebViewInstance(context)
        }
        val wv = webView!!
        (wv.parent as? ViewGroup)?.removeView(wv)
        return wv
    }

    private fun setupWebViewInstance(context: Context) {
        if (webView != null) return
        try {
            WebView.setWebContentsDebuggingEnabled(true)
            val wv = BackgroundWebView(context.applicationContext)
            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = false
                loadWithOverviewMode = false
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "HTML Player loaded successfully: $url")
                }
            }

            wv.webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap? {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d(TAG, "WebView Console: [${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                    return true
                }
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(wv, true)

            wv.addJavascriptInterface(PlayerBridge(), "AndroidBridge")
            val origin = "https://${context.packageName}"
            wv.loadDataWithBaseURL(origin, getPlayerHtml(context.packageName), "text/html", "UTF-8", null)
            webView = wv
            Log.d(TAG, "WebView background player initialized with origin $origin")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init WebView player: ${e.message}", e)
        }
    }

    fun playSong(song: Song, startPositionMs: Long = 0L) {
        _currentSong.value = song
        _isBuffering.value = true
        _currentPositionMs.value = startPositionMs
        _durationMs.value = song.durationSeconds * 1000L

        val startSeconds = (startPositionMs / 1000.0).coerceAtLeast(0.0)

        mainHandler.post {
            if (isPlayerReady && webView != null) {
                webView?.evaluateJavascript("playVideo('${song.id}', $startSeconds);", null)
            } else {
                pendingSongToPlay = song
                pendingStartPositionMs = startPositionMs
            }
        }

        // Đồng bộ lịch sử xem với YouTube chính thức
        YouTubeSyncManager.startSync(song.id, startSeconds)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        _currentSong.value?.let { song ->
            PlaybackProgressManager.saveProgress(song.id, _currentPositionMs.value, _durationMs.value)
        }
        mainHandler.post {
            webView?.evaluateJavascript("pause();", null)
        }
        YouTubeSyncManager.pauseSync()
    }

    fun stop() {
        _currentSong.value?.let { song ->
            PlaybackProgressManager.saveProgress(song.id, _currentPositionMs.value, _durationMs.value)
        }
        _isPlaying.value = false
        _isBuffering.value = false
        _currentSong.value = null
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        pendingSongToPlay = null
        pendingStartPositionMs = 0L
        onStateChangedListener?.invoke(false, null)
        mainHandler.post {
            webView?.evaluateJavascript("stop();", null)
        }
        YouTubeSyncManager.stopSync()
    }

    fun resume() {
        mainHandler.post {
            webView?.evaluateJavascript("resume();", null)
        }
        YouTubeSyncManager.resumeSync()
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        val seconds = positionMs / 1000.0
        mainHandler.post {
            webView?.evaluateJavascript("seek($seconds);", null)
        }
    }

    fun seekForward(deltaMs: Long = 10000L) {
        val target = (_currentPositionMs.value + deltaMs).coerceAtMost(_durationMs.value)
        seekTo(target)
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        val target = (_currentPositionMs.value - deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun setVolume(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        mainHandler.post {
            webView?.evaluateJavascript("setVolume($clamped);", null)
        }
    }

    private class PlayerBridge {
        @JavascriptInterface
        fun onReady() {
            Log.d(TAG, "YouTube IFrame API Ready")
            isPlayerReady = true
            mainHandler.post {
                pendingSongToPlay?.let {
                    playSong(it, pendingStartPositionMs)
                    pendingSongToPlay = null
                    pendingStartPositionMs = 0L
                }
            }
        }

        @JavascriptInterface
        fun onStateChange(state: Int, durationSec: Double) {
            Log.d(TAG, "Player state: $state, duration: $durationSec")
            mainHandler.post {
                // 1: PLAYING, 2: PAUSED, 3: BUFFERING, 0: ENDED
                when (state) {
                    1 -> {
                        _isPlaying.value = true
                        _isBuffering.value = false
                        if (durationSec > 0) _durationMs.value = (durationSec * 1000).toLong()
                        onStateChangedListener?.invoke(true, _currentSong.value)
                    }
                    2 -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        _currentSong.value?.let { song ->
                            PlaybackProgressManager.saveProgress(song.id, _currentPositionMs.value, _durationMs.value)
                        }
                        onStateChangedListener?.invoke(false, _currentSong.value)
                    }
                    3 -> {
                        _isBuffering.value = true
                    }
                    0 -> {
                        _isPlaying.value = false
                        _isBuffering.value = false
                        _currentSong.value?.let { song ->
                            PlaybackProgressManager.clearProgress(song.id)
                        }
                        onStateChangedListener?.invoke(false, _currentSong.value)
                        onSongEndedListener?.invoke()
                        YouTubeSyncManager.stopSync()
                    }
                }
            }
        }

        @JavascriptInterface
        fun onProgress(currentSec: Double, durationSec: Double) {
            mainHandler.post {
                val posMs = (currentSec * 1000).toLong()
                _currentPositionMs.value = posMs
                val durMs = if (durationSec > 0) (durationSec * 1000).toLong() else _durationMs.value
                if (durationSec > 0) {
                    _durationMs.value = durMs
                }
                _currentSong.value?.let { song ->
                    PlaybackProgressManager.saveProgress(song.id, posMs, durMs)
                }
                // Đồng bộ vị trí phát sang YouTube
                YouTubeSyncManager.syncPosition(currentSec)
            }
        }

        @JavascriptInterface
        fun onError(code: Int) {
            Log.e(TAG, "YouTube player error code: $code")
            mainHandler.post {
                _isBuffering.value = false
            }
        }
    }

    private fun getPlayerHtml(packageName: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                }
                html, body {
                    width: 100% !important;
                    height: 100% !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    background-color: #000000 !important;
                    overflow: hidden !important;
                }
                #player, iframe {
                    position: absolute !important;
                    top: 0 !important;
                    left: 0 !important;
                    width: 100% !important;
                    height: 100% !important;
                    border: none !important;
                    display: block !important;
                }
            </style>
            <script defer src="https://www.youtube.com/iframe_api"></script>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var player;
                var currentVideoId = '';
                var progressInterval = null;

                function resizePlayer(w, h) {
                    try {
                        var width = (w && w > 0) ? w : (window.innerWidth || document.documentElement.clientWidth);
                        var height = (h && h > 0) ? h : (window.innerHeight || document.documentElement.clientHeight);
                        var el = document.getElementById('player');
                        if (el) {
                            el.style.width = '100%';
                            el.style.height = '100%';
                        }
                        var ifr = document.querySelector('iframe');
                        if (ifr) {
                            ifr.style.width = '100%';
                            ifr.style.height = '100%';
                        }
                        if (player && player.setSize) {
                            player.setSize(width, height);
                        }
                    } catch(e) {}
                }

                window.addEventListener('resize', function() {
                    resizePlayer();
                });

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        playerVars: {
                            'autoplay': 0,
                            'controls': 0,
                            'enablejsapi': 1,
                            'fs': 0,
                            'origin': 'https://$packageName',
                            'rel': 0,
                            'iv_load_policy': 3,
                            'cc_load_policy': 0,
                            'playsinline': 1
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    });
                }

                function onPlayerReady(event) {
                    resizePlayer();
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onReady();
                    }
                    if (!progressInterval) {
                        progressInterval = setInterval(function() {
                            try {
                                if (player && player.getCurrentTime && player.getPlayerState && player.getPlayerState() === 1) {
                                    var curr = player.getCurrentTime() || 0;
                                    var dur = player.getDuration() || 0;
                                    if (window.AndroidBridge) {
                                        window.AndroidBridge.onProgress(curr, dur);
                                    }
                                }
                            } catch(e) {}
                        }, 500);
                    }
                }

                function onPlayerStateChange(event) {
                    var dur = 0;
                    try {
                        if (player && player.getDuration) dur = player.getDuration();
                    } catch(e) {}
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onStateChange(event.data, dur);
                    }
                }

                function onPlayerError(event) {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onError(event.data);
                    }
                }

                function playVideo(id, startSeconds) {
                    currentVideoId = id;
                    var start = startSeconds || 0;
                    if (player && player.loadVideoById) {
                        player.loadVideoById({
                            'videoId': id,
                            'startSeconds': start
                        });
                    }
                }

                function pause() {
                    if (player && player.pauseVideo) {
                        player.pauseVideo();
                    }
                }

                function stop() {
                    try {
                        if (player && player.stopVideo) {
                            player.stopVideo();
                        }
                    } catch(e) {}
                }

                function resume() {
                    if (player && player.playVideo) {
                        player.playVideo();
                    }
                }

                function seek(sec) {
                    if (player && player.seekTo) {
                        player.seekTo(sec, true);
                    }
                }

                function setVolume(vol) {
                    if (player && player.setVolume) {
                        player.setVolume(vol);
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
