package com.soundtube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soundtube.app.data.EpisodeManager
import com.soundtube.app.data.PlaybackProgressManager
import com.soundtube.app.data.model.Song
import com.soundtube.app.ui.theme.AccentGradient
import com.soundtube.app.ui.theme.AccentPurple
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.DarkBackground
import com.soundtube.app.ui.theme.DarkSurface
import com.soundtube.app.ui.theme.DarkSurfaceVariant
import com.soundtube.app.ui.theme.QuantumMagenta
import com.soundtube.app.ui.theme.QuantumViolet
import com.soundtube.app.ui.theme.TextMuted
import com.soundtube.app.ui.theme.TextPrimary
import com.soundtube.app.ui.theme.TextSecondary
import com.soundtube.app.ui.theme.neumorphic

@Composable
fun YouScreen(
    watchHistory: List<Song>,
    isLoggedIn: Boolean,
    userName: String?,
    userAvatar: String?,
    isAutoplayEnabled: Boolean,
    isSyncEnabled: Boolean,
    isDarkMode: Boolean = false,
    onToggleAutoplay: () -> Unit,
    onToggleSync: () -> Unit,
    onToggleDarkMode: () -> Unit = {},
    onPlaySong: (Song, List<Song>) -> Unit,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        // 1. Thẻ thông tin tài khoản người dùng (Khung trắng đục lăng kính lượng tử)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .neumorphic(shape = RoundedCornerShape(22.dp), elevation = 5.dp, quantumGlow = true)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(74.dp),
                contentAlignment = Alignment.Center
            ) {
                com.soundtube.app.ui.components.QuantumOrbitalHalo(size = 74.dp)
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .neumorphic(shape = CircleShape, elevation = 3.dp)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = userAvatar,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = if (isLoggedIn) AccentRed else TextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isLoggedIn) {
                    Text(
                        text = userName ?: "Tài khoản YouTube",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Đã đồng bộ Google Account",
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đăng xuất",
                        color = AccentRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onLogout() }
                    )
                } else {
                    Text(
                        text = "Chưa đăng nhập",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Đăng nhập để đồng bộ đề xuất & lịch sử",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentRed)
                            .clickable { onOpenLogin() }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Đăng nhập Google / YouTube",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Mục Lịch Sử Đã Xem
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lịch sử đã xem (${watchHistory.size})",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (watchHistory.isNotEmpty()) {
                Text(
                    text = "Xóa lịch sử",
                    color = AccentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onClearHistory() }
                )
            }
        }

        if (watchHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có video nào trong lịch sử nghe",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        } else {
            // Danh sách cuộn ngang các bài đã xem
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                watchHistory.forEach { song ->
                    val progressRatio = PlaybackProgressManager.getProgressRatio(song.id, song.durationSeconds)
                    val formattedProgress = PlaybackProgressManager.getFormattedProgress(song.id)
                    val epInfo = EpisodeManager.parseEpisode(song.title)

                    Column(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onPlaySong(song, watchHistory) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .neumorphic(shape = RoundedCornerShape(12.dp), elevation = 4.dp)
                                .padding(3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurface)
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Badge số tập ở góc trên bên trái nếu có
                            if (epInfo != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(AccentRed)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Tập ${epInfo.episodeNumber}",
                                        color = Color.White,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Badge đã nghe dở ở góc dưới bên trái
                            if (formattedProgress != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.Black.copy(alpha = 0.85f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = formattedProgress,
                                        color = Color(0xFFFF5252),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Badge thời lượng ở góc dưới bên phải
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.Black.copy(alpha = 0.8f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = song.formattedDuration,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Thanh tiến trình đỏ ở đáy thumbnail
                            if (progressRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .align(Alignment.BottomStart)
                                        .background(Color.White.copy(alpha = 0.3f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progressRatio)
                                            .height(3.dp)
                                            .background(AccentRed)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = song.title,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.uploaderName,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Tính Năng & Tùy Chọn
        Text(
            text = "Tính năng phát nhạc",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .neumorphic(shape = RoundedCornerShape(18.dp), elevation = 5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ProFeatureRow(
                    icon = Icons.Filled.Lock,
                    title = "Phát âm thanh nền",
                    desc = "Hỗ trợ phát âm thanh khi chuyển ứng dụng hoặc tắt màn hình"
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProFeatureRow(
                    icon = Icons.Filled.Shield,
                    title = "Trải nghiệm nghe liền mạch",
                    desc = "Tối ưu đường truyền và bộ nhớ đệm phát nhạc liên tục"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tự động phát tiếp (Auto-play)",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Hết bài sẽ tự động phát bài kế tiếp",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isAutoplayEnabled,
                        onCheckedChange = { onToggleAutoplay() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentRed,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = null,
                            tint = if (isDarkMode) QuantumViolet else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Giao diện tối (Dark mode)",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Nền không gian lượng tử dịu mắt & tiết kiệm pin",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = QuantumMagenta,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBackground
                        )
                    )
                }

                // Đồng bộ YouTube — chỉ hiện khi đã đăng nhập
                if (isLoggedIn) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                tint = if (isSyncEnabled) Color(0xFF4CAF50) else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Đồng bộ YouTube chính thức",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Lịch sử & thanh đỏ tự cập nhật trên YouTube",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = isSyncEnabled,
                            onCheckedChange = { onToggleSync() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4CAF50),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBackground
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Thông tin ứng dụng
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .neumorphic(shape = RoundedCornerShape(18.dp), elevation = 5.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "QuarkTube v1.0.0",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Trình phát âm thanh và video nền tối ưu hoá hiệu năng cho thiết bị Android.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ProFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AccentRed,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Hoạt động",
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
    }
}
