package com.soundtube.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak", "SetJavaScriptEnabled")
object AuthManager {
    private const val TAG = "AuthManager"
    private const val PREFS_NAME = "soundtube_auth_prefs"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_AVATAR = "user_avatar"

    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow<String?>("Người dùng")
    val userName: StateFlow<String?> = _userName.asStateFlow()

    private val _userAvatar = MutableStateFlow<String?>(null)
    val userAvatar: StateFlow<String?> = _userAvatar.asStateFlow()

    private var isFetchingProfile = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedLoggedIn = prefs?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false
            val cookiesPresent = checkYouTubeCookies()
            _isLoggedIn.value = savedLoggedIn || cookiesPresent

            val savedName = prefs?.getString(KEY_USER_NAME, null)
            val savedAvatar = prefs?.getString(KEY_USER_AVATAR, null)
            _userName.value = savedName ?: "Tài khoản YouTube"
            _userAvatar.value = savedAvatar

            // Nếu đã đăng nhập nhưng chưa có tên thật hoặc avatar, tự động fetch profile ngay
            if (_isLoggedIn.value && (savedName == null || savedName == "Người dùng YouTube" || savedName == "Tài khoản YouTube" || savedAvatar.isNullOrBlank())) {
                fetchUserProfile(context)
            }
        }
    }

    fun checkYouTubeCookies(): Boolean {
        val cookies = try {
            CookieManager.getInstance().getCookie("https://www.youtube.com") ?: ""
        } catch (_: Exception) { "" }
        val hasLoginCookie = cookies.contains("LOGIN_INFO") || 
                             cookies.contains("SAPISID") || 
                             cookies.contains("SSID") ||
                             cookies.contains("__Secure-3PAPISID") ||
                             cookies.contains("__Secure-1PAPISID")

        if (hasLoginCookie) {
            _isLoggedIn.value = true
            prefs?.edit()?.putBoolean(KEY_IS_LOGGED_IN, true)?.apply()

            // Nếu chưa có tên/avatar thì fetch
            val currentName = _userName.value
            val currentAvatar = _userAvatar.value
            if (currentName == null || currentName == "Người dùng" || currentName == "Người dùng YouTube" || currentName == "Tài khoản YouTube" || currentAvatar.isNullOrBlank()) {
                fetchUserProfile(appContext)
            }
        }
        return hasLoginCookie
    }

    fun setLoggedIn(name: String? = null, avatarUrl: String? = null) {
        _isLoggedIn.value = true
        if (!name.isNullOrBlank()) {
            _userName.value = name
        } else if (_userName.value == null || _userName.value == "Người dùng") {
            _userName.value = "Tài khoản YouTube"
        }

        if (!avatarUrl.isNullOrBlank()) {
            _userAvatar.value = avatarUrl
        }

        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            if (!name.isNullOrBlank()) putString(KEY_USER_NAME, name)
            if (!avatarUrl.isNullOrBlank()) putString(KEY_USER_AVATAR, avatarUrl)
            apply()
        }
        Log.d(TAG, "Auth state updated: name=${_userName.value}, avatar=${_userAvatar.value}")
    }

    fun fetchUserProfile(context: Context? = null) {
        if (isFetchingProfile) return
        isFetchingProfile = true

        scope.launch {
            try {
                Log.d(TAG, "Starting profile fetch...")
                // Cách 1: Gọi InnerTube API account_menu
                val (apiName, apiAvatar) = fetchViaInnerTube()
                if (!apiName.isNullOrBlank() || !apiAvatar.isNullOrBlank()) {
                    setLoggedIn(apiName, apiAvatar)
                    isFetchingProfile = false
                    return@launch
                }

                // Cách 2: Gọi HTTP GET m.youtube.com để lấy HTML và parse ytInitialData
                val (htmlName, htmlAvatar) = fetchViaMobileHtml()
                if (!htmlName.isNullOrBlank() || !htmlAvatar.isNullOrBlank()) {
                    setLoggedIn(htmlName, htmlAvatar)
                    isFetchingProfile = false
                    return@launch
                }

                // Cách 3: Dùng WebView ẩn để load YouTube và chạy JS scraping
                val ctx = context ?: appContext
                if (ctx != null) {
                    mainHandler.post {
                        fetchViaHiddenWebView(ctx)
                    }
                } else {
                    isFetchingProfile = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in fetchUserProfile: ${e.message}", e)
                isFetchingProfile = false
            }
        }
    }

    private suspend fun fetchViaInnerTube(): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val cookies = CookieManager.getInstance().getCookie("https://www.youtube.com") ?: return@withContext Pair(null, null)
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
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val reqBuilder = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/account/account_menu")
                .post(requestBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/")
                .header("Cookie", cookies)
                .header("X-Youtube-Client-Name", "1")
                .header("X-Youtube-Client-Version", "2.20240101.01.00")

            if (!sapisid.isNullOrBlank()) {
                val timestamp = System.currentTimeMillis() / 1000
                val hashInput = "$timestamp $sapisid https://www.youtube.com"
                val md = MessageDigest.getInstance("SHA-1")
                val digest = md.digest(hashInput.toByteArray(Charsets.UTF_8))
                val hex = digest.joinToString("") { "%02x".format(it) }
                reqBuilder.header("Authorization", "SAPISIDHASH ${timestamp}_${hex}")
                reqBuilder.header("X-Origin", "https://www.youtube.com")
            }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "account_menu HTTP ${response.code}, size: ${body.length}")

            if (response.isSuccessful && body.isNotEmpty()) {
                val (name, avatar) = parseProfileFromJsonOrRegex(body)
                if (!name.isNullOrBlank() || !avatar.isNullOrBlank()) {
                    Log.d(TAG, "Found profile via InnerTube: name=$name, avatar=$avatar")
                    return@withContext Pair(name, avatar)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchViaInnerTube failed: ${e.message}")
        }
        Pair(null, null)
    }

    private suspend fun fetchViaMobileHtml(): Pair<String?, String?> = withContext(Dispatchers.IO) {
        try {
            val cookies = CookieManager.getInstance().getCookie("https://www.youtube.com") ?: return@withContext Pair(null, null)
            val request = Request.Builder()
                .url("https://m.youtube.com/")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
                .header("Cookie", cookies)
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "m.youtube.com HTTP ${response.code}, size: ${body.length}")

            if (response.isSuccessful && body.isNotEmpty()) {
                val (name, avatar) = parseProfileFromJsonOrRegex(body)
                if (!name.isNullOrBlank() || !avatar.isNullOrBlank()) {
                    Log.d(TAG, "Found profile via HTML: name=$name, avatar=$avatar")
                    return@withContext Pair(name, avatar)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchViaMobileHtml failed: ${e.message}")
        }
        Pair(null, null)
    }

    private fun parseProfileFromJsonOrRegex(content: String): Pair<String?, String?> {
        var name: String? = null
        var avatar: String? = null

        // 1. Regex tìm tên trong activeAccountHeaderRenderer
        val namePatterns = listOf(
            Regex("\"activeAccountHeaderRenderer\"\\s*:\\s*\\{[^}]*\"accountName\"\\s*:\\s*\\{\\s*\"simpleText\"\\s*:\\s*\"([^\"]+)\""),
            Regex("\"activeAccountHeaderRenderer\"\\s*:\\s*\\{[^}]*\"accountName\"\\s*:\\s*\\{\\s*\"runs\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"([^\"]+)\""),
            Regex("\"channelHandle\"\\s*:\\s*\\{\\s*\"runs\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"([^\"]+)\""),
            Regex("\"channelName\"\\s*:\\s*\"([^\"]+)\""),
            Regex("\"DELEGATED_SESSION_ID\"[^\"]+\"title\"\\s*:\\s*\"([^\"]+)\"")
        )

        for (pattern in namePatterns) {
            val m = pattern.find(content)
            if (m != null && m.groupValues[1].isNotBlank()) {
                name = m.groupValues[1].replace("\\u0026", "&")
                break
            }
        }

        // 2. Regex tìm avatar ảnh yt3.ggpht hoặc lh3.googleusercontent
        val avatarPatterns = listOf(
            Regex("\"activeAccountHeaderRenderer\"\\s*:\\s*\\{[^}]*\"url\"\\s*:\\s*\"(https://[^\"]+)\""),
            Regex("\"(https://yt3\\.ggpht\\.com/[a-zA-Z0-9_-]+=[sS]\\d+[^\"\\s]*)\""),
            Regex("\"(https://yt3\\.ggpht\\.com/[^\"]+)\""),
            Regex("\"(https://lh3\\.googleusercontent\\.com/[a-zA-Z0-9_-]+=[sS]\\d+[^\"\\s]*)\""),
            Regex("\"(https://lh3\\.googleusercontent\\.com/[^\"]+)\"")
        )

        for (pattern in avatarPatterns) {
            val m = pattern.find(content)
            if (m != null && m.groupValues[1].isNotBlank()) {
                var url = m.groupValues[1].replace("\\u0026", "&")
                // Đổi kích thước ảnh lên 240px
                url = url.replace(Regex("=[sS]\\d+"), "=s240")
                avatar = url
                break
            }
        }

        return Pair(name, avatar)
    }

    private var hiddenProfileWebView: WebView? = null

    private fun fetchViaHiddenWebView(context: Context) {
        try {
            if (hiddenProfileWebView != null) {
                hiddenProfileWebView?.destroy()
                hiddenProfileWebView = null
            }

            val wv = WebView(context)
            hiddenProfileWebView = wv

            wv.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            wv.addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onProfile(name: String?, avatar: String?) {
                        Log.d(TAG, "Hidden WebView extracted: name=$name, avatar=$avatar")
                        mainHandler.post {
                            if (!name.isNullOrBlank() || !avatar.isNullOrBlank()) {
                                setLoggedIn(name, avatar)
                            }
                            isFetchingProfile = false
                            try {
                                hiddenProfileWebView?.destroy()
                                hiddenProfileWebView = null
                            } catch (_: Exception) {}
                        }
                    }
                },
                "AndroidHiddenProfile"
            )

            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Hidden WebView page finished: $url")

                    // Inject JS sau 1.5s
                    view?.postDelayed({
                        val js = """
                            (function() {
                                try {
                                    var name = '';
                                    var avatar = '';

                                    // Kiểm tra ytInitialData
                                    if (typeof ytInitialData !== 'undefined') {
                                        var raw = JSON.stringify(ytInitialData);
                                        var mName = raw.match(/"accountName":\{"simpleText":"([^"]+)"/) || raw.match(/"accountName":\{"runs":\[\{"text":"([^"]+)"/);
                                        if (mName) name = mName[1];

                                        var mAvatar = raw.match(/"(https:\/\/yt3\.ggpht\.com\/[^"]+)"/) || raw.match(/"(https:\/\/lh3\.googleusercontent\.com\/[^"]+)"/);
                                        if (mAvatar) avatar = mAvatar[1].replace(/\\u0026/g, '&');
                                    }

                                    // Kiểm tra các phần tử DOM
                                    if (!avatar) {
                                        var imgs = document.querySelectorAll('button[aria-label] img, #avatar-btn img, img.yt-spec-avatar-shape__avatar, yt-img-shadow img');
                                        for (var i = 0; i < imgs.length; i++) {
                                            var src = imgs[i].src || imgs[i].getAttribute('src');
                                            if (src && (src.indexOf('googleusercontent.com') !== -1 || src.indexOf('yt3.ggpht.com') !== -1)) {
                                                avatar = src.replace(/=s\d+/, '=s240');
                                                break;
                                            }
                                        }
                                    }

                                    AndroidHiddenProfile.onProfile(name, avatar);
                                } catch(e) {
                                    AndroidHiddenProfile.onProfile('', '');
                                }
                            })();
                        """.trimIndent()
                        view.evaluateJavascript(js, null)
                    }, 1500)

                    // Timeout an toàn 6s
                    view?.postDelayed({
                        if (isFetchingProfile) {
                            isFetchingProfile = false
                            try {
                                hiddenProfileWebView?.destroy()
                                hiddenProfileWebView = null
                            } catch (_: Exception) {}
                        }
                    }, 6000)
                }
            }

            wv.loadUrl("https://m.youtube.com/")
        } catch (e: Exception) {
            Log.e(TAG, "Hidden WebView error: ${e.message}", e)
            isFetchingProfile = false
        }
    }

    fun logout() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        _isLoggedIn.value = false
        _userName.value = null
        _userAvatar.value = null

        prefs?.edit()?.clear()?.apply()
    }
}
