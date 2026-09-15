package com.soundtube.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.soundtube.app.MainActivity
import com.soundtube.app.R
import com.soundtube.app.data.model.Song
import com.soundtube.app.player.BackgroundPlayerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL

class PlaybackService : Service() {
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "soundtube_playback_channel"
        const val ACTION_PLAY = "com.soundtube.app.ACTION_PLAY"
        const val ACTION_PAUSE = "com.soundtube.app.ACTION_PAUSE"
        const val ACTION_STOP = "com.soundtube.app.ACTION_STOP"
        const val ACTION_NEXT = "com.soundtube.app.ACTION_NEXT"
        const val ACTION_PREV = "com.soundtube.app.ACTION_PREV"

        var onNextListener: (() -> Unit)? = null
        var onPrevListener: (() -> Unit)? = null
    }

    private var mediaSession: MediaSessionCompat? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var lastThumbnailBitmap: Bitmap? = null
    private var lastThumbnailUrl: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 1. Giữ WakeLock để CPU điện thoại không bị dừng khi tắt màn hình
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundTube:AudioPlayback").apply {
            setReferenceCounted(false)
        }

        // 2. Khởi tạo MediaSessionCompat cho Lock Screen & Bluetooth
        mediaSession = MediaSessionCompat(this, "SoundTubeMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    BackgroundPlayerManager.resume()
                }

                override fun onPause() {
                    BackgroundPlayerManager.pause()
                }

                override fun onSkipToNext() {
                    onNextListener?.invoke()
                }

                override fun onSkipToPrevious() {
                    onPrevListener?.invoke()
                }

                override fun onStop() {
                    BackgroundPlayerManager.stop()
                }
            })
            isActive = true
        }

        // 3. Lắng nghe trạng thái từ BackgroundPlayerManager
        BackgroundPlayerManager.onStateChangedListener = { isPlaying, song ->
            if (isPlaying) {
                if (wakeLock?.isHeld == false) wakeLock?.acquire(24 * 60 * 60 * 1000L) // Giữ wakeLock tối đa 24h
            } else {
                if (wakeLock?.isHeld == true) wakeLock?.release()
            }
            updateNotification(isPlaying, song)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Luôn gọi startForeground ngay lập tức để thỏa mãn yêu cầu của Android 8+ và Android 14+
        startForegroundImmediately()

        when (intent?.action) {
            ACTION_PLAY -> BackgroundPlayerManager.resume()
            ACTION_PAUSE -> BackgroundPlayerManager.pause()
            ACTION_NEXT -> onNextListener?.invoke()
            ACTION_PREV -> onPrevListener?.invoke()
            ACTION_STOP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundImmediately() {
        val song = BackgroundPlayerManager.currentSong.value
        val isPlaying = BackgroundPlayerManager.isPlaying.value
        val notification = if (song != null) {
            buildNotification(isPlaying, song, lastThumbnailBitmap)
        } else {
            buildPlaceholderNotification()
        }
        startForegroundWithNotification(notification)
    }

    private fun startForegroundWithNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildPlaceholderNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuarkTube")
            .setContentText("Đang phát nhạc...")
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentIntent(openAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(isPlaying: Boolean, song: Song?) {
        if (song == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return
        }

        // 1. Cập nhật ngay notification hiện tại
        val currentNotification = buildNotification(isPlaying, song, lastThumbnailBitmap)
        startForegroundWithNotification(currentNotification)

        // 2. Tải ảnh thumbnail ở background nếu URL đổi
        if (song.thumbnailUrl.isNotBlank() && song.thumbnailUrl != lastThumbnailUrl) {
            serviceScope.launch {
                try {
                    val url = URL(song.thumbnailUrl)
                    val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    if (bitmap != null) {
                        lastThumbnailBitmap = bitmap
                        lastThumbnailUrl = song.thumbnailUrl
                        val updatedNotification = buildNotification(isPlaying, song, lastThumbnailBitmap)
                        val manager = getSystemService(NotificationManager::class.java)
                        manager?.notify(NOTIFICATION_ID, updatedNotification)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun buildNotification(isPlaying: Boolean, song: Song, artwork: Bitmap?): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val prevIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(this, 4, prevIntent, PendingIntent.FLAG_IMMUTABLE)
        val prevAction = NotificationCompat.Action(android.R.drawable.ic_media_previous, "Bài trước", prevPendingIntent)

        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Tạm dừng", pausePendingIntent)
        } else {
            val playIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PLAY }
            val playPendingIntent = PendingIntent.getService(this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Phát", playPendingIntent)
        }

        val nextIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 5, nextIntent, PendingIntent.FLAG_IMMUTABLE)
        val nextAction = NotificationCompat.Action(android.R.drawable.ic_media_next, "Bài tiếp", nextPendingIntent)

        val stopIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Đóng", stopPendingIntent)

        // Cập nhật Metadata cho MediaSession
        val metaBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.uploaderName)
        if (artwork != null) {
            metaBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
        }
        mediaSession?.setMetadata(metaBuilder.build())

        // Cập nhật PlaybackState
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, BackgroundPlayerManager.currentPositionMs.value, 1f)
                .build()
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song.title)
            .setContentText(song.uploaderName)
            .setSubText("QuarkTube - Phát khi tắt màn hình")
            .setSmallIcon(R.drawable.ic_notification_small)
            .setLargeIcon(artwork)
            .setContentIntent(openAppIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(isPlaying)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SoundTube Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Điều khiển nhạc trên màn hình khóa khi tắt màn hình"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        mediaSession?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
