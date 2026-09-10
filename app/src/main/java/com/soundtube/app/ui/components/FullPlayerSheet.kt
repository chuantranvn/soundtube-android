package com.soundtube.app.ui.components
import android.view.ViewGroup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.soundtube.app.player.BackgroundPlayerManager
import com.soundtube.app.data.EpisodeManager
import com.soundtube.app.data.PlaybackProgressManager
import com.soundtube.app.data.model.Song
import com.soundtube.app.ui.theme.AccentPurple
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.DarkBackground
import com.soundtube.app.ui.theme.DarkSurface
import com.soundtube.app.ui.theme.DarkSurfaceVariant
import com.soundtube.app.ui.theme.TextMuted
import com.soundtube.app.ui.theme.TextPrimary
import com.soundtube.app.ui.theme.TextSecondary

@Composable
fun FullPlayerSheet(
    isVisible: Boolean,
    song: Song?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    volume: Float = 0.7f,
    isAutoplayEnabled: Boolean = true,
    isVideoMode: Boolean = true,
    nextEpisode: Song? = null,
    seriesEpisodes: List<Song> = emptyList(),
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onToggleAutoplay: () -> Unit = {},
    onToggleVideoMode: () -> Unit = {},
    onEnterFullscreen: () -> Unit = {},
    onVolumeChanged: (Float) -> Unit = {},
    onSeekTo: (Long) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onPlaySong: (Song) -> Unit = {},
    onRestartFromBeginning: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = isVisible && song != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        if (song == null) return@AnimatedVisibility

        var isDraggingSlider by remember { mutableStateOf(false) }
        var sliderPosition by remember { mutableFloatStateOf(0f) }

        val actualProgress = if (durationMs > 0) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        val displayProgress = if (isDraggingSlider) sliderPosition else actualProgress

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkSurfaceVariant,
                            DarkBackground,
                            DarkBackground
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Nút thu nhỏ & Tiêu đề
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Thu nhỏ",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "ĐANG PHÁT TỪ YOUTUBE",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bộ chọn chuyển đổi chế độ [ Âm thanh ] / [ Video ] chuẩn YouTube Music
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF242424))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isVideoMode) AccentRed else Color.Transparent)
                            .clickable { if (isVideoMode) onToggleVideoMode() }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Âm thanh",
                            color = if (!isVideoMode) Color.White else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = if (!isVideoMode) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isVideoMode) AccentRed else Color.Transparent)
                            .clickable { if (!isVideoMode) onToggleVideoMode() }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if (isVideoMode) Color.White else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Video",
                                color = if (isVideoMode) Color.White else TextMuted,
                                fontSize = 13.sp,
                                fontWeight = if (isVideoMode) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vùng hiển thị Video trực tiếp hoặc Ảnh bìa Album
                if (isVideoMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .shadow(16.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { ctx ->
                                    val wv = BackgroundPlayerManager.getOrCreateWebView(ctx)
                                    (wv.parent as? ViewGroup)?.removeView(wv)
                                    wv.isClickable = false
                                    wv.isFocusable = false
                                    wv
                                },
                                update = { wv ->
                                    wv.isClickable = false
                                    wv.isFocusable = false
                                    wv.post {
                                        wv.evaluateJavascript("if (window.resizePlayer) { window.resizePlayer(); }", null)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Lớp phủ chạm để Tạm dừng / Tiếp tục phát
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onTogglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isBuffering) {
                                    CircularProgressIndicator(
                                        color = AccentRed,
                                        modifier = Modifier.size(42.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }

                            // Nút phóng to toàn màn hình ở góc dưới bên phải
                            IconButton(
                                onClick = onEnterFullscreen,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .size(44.dp)
                                    .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                                    .zIndex(10f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fullscreen,
                                    contentDescription = "Toàn màn hình",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Ảnh bìa Album lớn với hiệu ứng đổ bóng & nút chuyển sang Video
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(1f)
                    ) {
                        AsyncImage(
                            model = song.thumbnailUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(16.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onToggleVideoMode() }
                        )

                        // Nút badge nhỏ gợi ý xem video
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xCC000000))
                                .clickable { onToggleVideoMode() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Chuyển sang xem video",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Xem Video",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tên bài hát & Nghệ sĩ
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = song.uploaderName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AccentRed,
                    fontWeight = FontWeight.Medium
                )

                // Nhắc nhở tiếp tục nghe dở từ phút/giây lần trước
                val formattedResumeTime = remember(song.id) {
                    PlaybackProgressManager.getFormattedProgress(song.id)
                }
                if (formattedResumeTime != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2E1A1A))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đang tiếp tục từ $formattedResumeTime",
                            color = Color(0xFFFFD2D2),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• Nghe lại từ đầu",
                            color = Color(0xFFFF6E6E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onRestartFromBeginning() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Thanh tua nhạc (Seekbar Slider)
                Slider(
                    value = displayProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        val targetMs = (sliderPosition * durationMs).toLong()
                        onSeekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentRed,
                        activeTrackColor = AccentRed,
                        inactiveTrackColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Hiển thị thời gian (Hiện tại / Tổng thời lượng)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentSec = if (isDraggingSlider) {
                        (sliderPosition * durationMs / 1000).toLong()
                    } else {
                        currentPositionMs / 1000
                    }
                    val totalSec = durationMs / 1000

                    Text(
                        text = formatTime(currentSec),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatTime(totalSec),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hàng nút điều khiển: Prev - Lùi 10s - Play/Pause - Tiến 10s - Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Bài trước",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(onClick = onSeekBackward) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Tua lùi 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Nút Play / Pause tròn to nổi bật
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AccentRed, AccentPurple)
                                )
                            )
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            IconButton(onClick = onTogglePlayPause) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = onSeekForward) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Tua tới 10s",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Bài tiếp theo",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thanh điều chỉnh âm lượng (Volume Slider)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeDown,
                        contentDescription = "Giảm âm lượng",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChanged,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentRed,
                            activeTrackColor = AccentRed,
                            inactiveTrackColor = DarkSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = "Tăng âm lượng",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Tự động phát tiếp bài kế tiếp (Autoplay Switch)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Autorenew,
                            contentDescription = null,
                            tint = if (isAutoplayEnabled) AccentRed else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tự động phát tiếp (Auto-play)",
                            color = if (isAutoplayEnabled) TextPrimary else TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Switch(
                        checked = isAutoplayEnabled,
                        onCheckedChange = { onToggleAutoplay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentRed,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card nhắc nhở tính năng phát khi tắt màn hình
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Text(
                        text = "🔒 Bạn có thể khóa/tắt màn hình hoặc dùng app khác, âm thanh vẫn phát ngầm mượt mà!",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Đề xuất Tập kế tiếp (Dành cho nghe truyện / audiobook / series)
                if (nextEpisode != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPlaySong(nextEpisode) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF241515))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = null,
                                    tint = AccentRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "TẬP TIẾP THEO (TỰ ĐỘNG PHÁT KHI HẾT)",
                                    color = AccentRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 86.dp, height = 52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                ) {
                                    AsyncImage(
                                        model = nextEpisode.thumbnailUrl,
                                        contentDescription = nextEpisode.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = nextEpisode.title,
                                        color = TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = nextEpisode.uploaderName,
                                        color = TextSecondary,
                                        fontSize = 11.5.sp,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AccentRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Phát ngay",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Danh sách các tập của bộ truyện (nếu tìm thấy nhiều tập)
                if (seriesEpisodes.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    var isSeriesExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSeriesExpanded = !isSeriesExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.PlaylistPlay,
                                        contentDescription = null,
                                        tint = AccentRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Danh sách các tập (${seriesEpisodes.size} tập)",
                                        color = TextPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    imageVector = if (isSeriesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (isSeriesExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    seriesEpisodes.forEach { ep ->
                                        val isCurrent = ep.id == song.id
                                        val epInfo = remember(ep.title) { EpisodeManager.parseEpisode(ep.title) }
                                        val epBadge = if (epInfo != null) "Tập ${epInfo.episodeNumber}" else "Tập"

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) Color(0xFF381515) else Color.Transparent)
                                                .clickable { onPlaySong(ep) }
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isCurrent) AccentRed else Color(0xFF2C2C2C))
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = epBadge,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ep.title,
                                                    color = if (isCurrent) Color(0xFFFF8A80) else TextPrimary,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (isCurrent) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Đang phát",
                                                    color = AccentRed,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    if (seconds <= 0) return "00:00"
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
