package com.soundtube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtube.app.data.model.Song
import com.soundtube.app.ui.components.YouTubeVideoCard
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.DarkBackground
import com.soundtube.app.ui.theme.DarkSurface
import com.soundtube.app.ui.theme.TextPrimary
import com.soundtube.app.ui.theme.TextSecondary

data class ChannelItem(
    val name: String,
    val initial: String,
    val color: Color
)

@Composable
fun SubscriptionsScreen(
    videoList: List<Song>,
    isLoading: Boolean,
    onPlayVideo: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val topChannels = listOf(
        ChannelItem("Tất cả", "★", AccentRed),
        ChannelItem("Sơn Tùng M-TP", "S", Color(0xFF1E88E5)),
        ChannelItem("Đen Vâu", "Đ", Color(0xFF43A047)),
        ChannelItem("Phố Nhạc Chill", "P", Color(0xFFFB8C00)),
        ChannelItem("HD LKT", "H", Color(0xFF8E24AA)),
        ChannelItem("H2O Remix", "R", Color(0xFFE53935)),
        ChannelItem("Miu Music", "M", Color(0xFF00ACC1))
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Dải danh sách kênh đăng ký cuộn ngang
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            topChannels.forEach { channel ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(62.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(channel.color)
                    ) {
                        Text(
                            text = channel.initial,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = channel.name,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bảng tin video mới nhất từ các kênh đã đăng ký
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(videoList, key = { it.id }) { video ->
                    YouTubeVideoCard(
                        song = video,
                        onClick = { onPlayVideo(video) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}
