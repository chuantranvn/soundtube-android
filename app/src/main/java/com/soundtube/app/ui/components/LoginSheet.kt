package com.soundtube.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.soundtube.app.data.AuthManager
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.DarkBackground
import com.soundtube.app.ui.theme.DarkSurface
import com.soundtube.app.ui.theme.TextPrimary
import com.soundtube.app.ui.theme.TextSecondary

private const val LOGIN_URL = "https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https%3A%2F%2Fm.youtube.com%2F"
private const val DESKTOP_CHROME_UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
private const val TAG = "LoginSheet"

/**
 * JavaScript bridge để nhận tên và avatar từ YouTube page.
 */
private class ProfileBridge(private val onProfileReceived: (String?, String?) -> Unit) {
    @JavascriptInterface
    fun onProfile(name: String?, avatar: String?) {
        Log.d(TAG, "Profile received: name=$name, avatar=$avatar")
        if (!name.isNullOrBlank() || !avatar.isNullOrBlank()) {
            onProfileReceived(name, avatar)
        }
    }
}

/**
 * JS script inject vào YouTube page để trích xuất tên + avatar người dùng.
 * Quét activeAccountHeaderRenderer, ytInitialData, DOM img và script tags.
 */
private val EXTRACT_PROFILE_JS = """
(function() {
    try {
        var name = null;
        var avatar = null;

        // Cách 1: Quét activeAccountHeaderRenderer từ ytInitialData
        if (typeof ytInitialData !== 'undefined') {
            try {
                var raw = JSON.stringify(ytInitialData);
                var mName = raw.match(/"accountName":\{"simpleText":"([^"]+)"/) || 
                            raw.match(/"accountName":\{"runs":\[\{"text":"([^"]+)"/) ||
                            raw.match(/"channelName":"([^"]+)"/);
                if (mName) name = mName[1];

                var mAvatar = raw.match(/"(https:\/\/yt3\.ggpht\.com\/[^\"]+)"/) || 
                              raw.match(/"(https:\/\/lh3\.googleusercontent\.com\/[^\"]+)"/);
                if (mAvatar) avatar = mAvatar[1].replace(/\\u0026/g, '&');
            } catch(e) {}
        }

        // Cách 2: Lấy avatar từ account button img trên DOM
        if (!avatar) {
            var imgs = document.querySelectorAll('button[aria-label] img, #avatar-btn img, img.yt-spec-avatar-shape__avatar, yt-img-shadow img, img.channel-thumbnail');
            for (var i = 0; i < imgs.length; i++) {
                var src = imgs[i].src || imgs[i].getAttribute('src');
                if (src && (src.indexOf('googleusercontent.com') !== -1 || src.indexOf('yt3.ggpht.com') !== -1)) {
                    avatar = src.replace(/=s\d+/, '=s240');
                    break;
                }
            }
        }

        // Cách 3: Lấy tên từ DOM
        if (!name) {
            var nameEls = document.querySelectorAll('#account-name, .channel-name, yt-formatted-string#channel-title');
            for (var i = 0; i < nameEls.length; i++) {
                var txt = nameEls[i].textContent.trim();
                if (txt && txt.length > 0 && txt.length < 80) {
                    name = txt;
                    break;
                }
            }
        }

        if (name || avatar) {
            AndroidProfile.onProfile(name || '', avatar || '');
        }
    } catch(e) {
        // Không gửi rỗng nếu có lỗi để tránh ghi đè
    }
})();
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginSheet(
    isVisible: Boolean,
    onClose: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var hasExtractedProfile by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewInstance?.stopLoading()
                webViewInstance?.loadUrl("about:blank")
                webViewInstance?.destroy()
            } catch (_: Exception) {}
            webViewInstance = null
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Đóng",
                        tint = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Đăng nhập YouTube",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Đồng bộ tài khoản & đề xuất",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(onClick = { webViewInstance?.reload() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Tải lại",
                        tint = TextPrimary
                    )
                }
            }

            // Thanh tiến trình tải trang web
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { webProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = AccentRed,
                    trackColor = DarkSurface
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewInstance = this

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                userAgentString = DESKTOP_CHROME_UA
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }

                            // Bridge JS → Kotlin để nhận tên + avatar
                            addJavascriptInterface(
                                ProfileBridge { name, avatar ->
                                    val finalName = if (!name.isNullOrBlank()) name else null
                                    val finalAvatar = if (!avatar.isNullOrBlank()) avatar else null
                                    Log.d(TAG, "Setting profile: name=$finalName, avatar=$finalAvatar")
                                    AuthManager.setLoggedIn(finalName, finalAvatar)
                                },
                                "AndroidProfile"
                            )

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    webProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    cookieManager.flush()

                                    // Kiểm tra xem đã đăng nhập thành công chưa
                                    val loggedIn = AuthManager.checkYouTubeCookies()
                                    if (loggedIn && !hasExtractedProfile) {
                                        hasExtractedProfile = true

                                        // Extract profile ngay lập tức (YouTube mobile page đã load)
                                        Log.d(TAG, "Injecting profile extraction JS into: $url")
                                        view?.evaluateJavascript(EXTRACT_PROFILE_JS, null)

                                        // Delay đóng LoginSheet 3 giây để WebView còn sống khi JS bridge trả về
                                        view?.postDelayed({
                                            // Thử extract lần 2 (phòng page render chậm)
                                            view.evaluateJavascript(EXTRACT_PROFILE_JS, null)
                                            // Đóng sheet sau khi extract xong
                                            view.postDelayed({
                                                onLoginSuccess()
                                            }, 500)
                                        }, 2500)
                                    }
                                }
                            }

                            loadUrl(LOGIN_URL)
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    }
                )
            }
        }
    }
}
