package com.soundtube.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soundtube.app.data.EpisodeManager
import com.soundtube.app.data.PlaybackProgressManager
import com.soundtube.app.data.model.Song
import com.soundtube.app.ui.theme.AccentGradient
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.DarkBackground
import com.soundtube.app.ui.theme.DarkSurface
import com.soundtube.app.ui.theme.QuantumSpectrumBrush
import com.soundtube.app.ui.theme.TextMuted
import com.soundtube.app.ui.theme.TextPrimary
import com.soundtube.app.ui.theme.TextSecondary
import com.soundtube.app.ui.theme.neumorphic

@Composable
fun YouTubeVideoCard(
    song: Song,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Đăng ký nhận cập nhật khi tiến trình phát thay đổi
    val progressUpdateTick by PlaybackProgressManager.progressUpdated.collectAsState()
    val progressRatio = remember(song.id, progressUpdateTick) {
        PlaybackProgressManager.getProgressRatio(song.id, song.durationSeconds)
    }
    val formattedProgress = remember(song.id, progressUpdateTick) {
        PlaybackProgressManager.getFormattedProgress(song.id)
    }
    val episodeInfo = remember(song.title) {
        EpisodeManager.parseEpisode(song.title)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 6.dp, bottom = 18.dp)
    ) {
        // 1. Thumbnail lớn tỷ lệ 16:9, đặt trong khung neumorphic lăng kính lượng tử
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .aspectRatio(16f / 9f)
                .neumorphic(shape = RoundedCornerShape(20.dp), elevation = 6.dp, quantumGlow = true)
                .padding(5.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(DarkSurface)
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Badge Số tập (VD: "TẬP 12") ở góc trên bên trái mang dải màu lượng tử
            if (episodeInfo != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGradient)
                        .padding(horizontal = 8.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = "⚛ TẬP ${episodeInfo.episodeNumber}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Badge Đã nghe đến mm:ss ở góc dưới bên trái
            if (formattedProgress != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 8.dp, bottom = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Đã nghe $formattedProgress",
                        color = Color(0xFFFF5252),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Badge thời lượng ở góc dưới bên phải (VD: 46:44)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = song.formattedDuration,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Thanh tiến trình quang phổ lượng tử (Hồng -> Tím -> Cyan) chạy dọc đáy thumbnail
            if (progressRatio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressRatio)
                            .height(4.dp)
                            .background(QuantumSpectrumBrush)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Thông tin: Avatar kênh, Tiêu đề 2 dòng, Kênh · Lượt xem · Thời gian, 3 chấm
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar kênh tròn (viền neumorphic nổi)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .neumorphic(shape = CircleShape, elevation = 3.dp)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                if (!song.channelAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.channelAvatarUrl,
                        contentDescription = song.uploaderName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Tiêu đề & Thông tin kênh
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                val subtitle = buildString {
                    append(song.uploaderName)
                    if (!song.viewCountText.isNullOrBlank()) {
                        append(" • ")
                        append(song.viewCountText)
                    }
                    if (!song.publishedTimeText.isNullOrBlank()) {
                        append(" • ")
                        append(song.publishedTimeText)
                    }
                }

                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Nút 3 chấm menu thao tác thực tế (Chia sẻ & Sao chép liên kết)
            Box {
                var menuExpanded by remember { mutableStateOf(false) }
                val context = LocalContext.current

                IconButton(
                    onClick = {
                        onMoreClick()
                        menuExpanded = true
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Thao tác",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Chia sẻ", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = TextPrimary)
                        },
                        onClick = {
                            menuExpanded = false
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, song.title)
                                    putExtra(Intent.EXTRA_TEXT, "${song.title}\nhttps://youtu.be/${song.id}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ"))
                            } catch (_: Exception) {}
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sao chép liên kết", color = TextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = TextPrimary)
                        },
                        onClick = {
                            menuExpanded = false
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Link", "https://youtu.be/${song.id}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Đã sao chép liên kết", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }
}
