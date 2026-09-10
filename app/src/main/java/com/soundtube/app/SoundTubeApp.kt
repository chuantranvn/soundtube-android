package com.soundtube.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.soundtube.app.data.AuthManager
import com.soundtube.app.sync.YouTubeSyncManager

class SoundTubeApp : Application() {
    companion object {
        const val CHANNEL_ID = "soundtube_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Khởi tạo AuthManager để kiểm tra cookie & tự động tải tên/avatar người dùng
        AuthManager.initialize(this)
        // Khởi tạo WebView ẩn để đồng bộ lịch sử xem với YouTube chính thức
        YouTubeSyncManager.initialize(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW // Low để không kêu tiếng chuông khi đổi bài hát
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
