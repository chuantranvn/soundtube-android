package com.soundtube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soundtube.app.ui.theme.DarkBackground
import com.soundtube.app.ui.theme.DarkSurface
import com.soundtube.app.ui.theme.TextMuted
import com.soundtube.app.ui.theme.TextPrimary

enum class YouTubeNavTab {
    HOME,
    SHORTS,
    CREATE,
    SUBSCRIPTIONS,
    YOU
}

@Composable
fun YouTubeBottomNav(
    selectedTab: YouTubeNavTab,
    onTabSelected: (YouTubeNavTab) -> Unit,
    userAvatarUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .border(width = 0.5.dp, color = DarkSurface)
            .navigationBarsPadding()
            .height(52.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Trang chủ
        NavItem(
            icon = Icons.Filled.Home,
            label = "Trang chủ",
            isSelected = selectedTab == YouTubeNavTab.HOME,
            onClick = { onTabSelected(YouTubeNavTab.HOME) }
        )

        // 2. Shorts
        NavItem(
            icon = Icons.Filled.FlashOn,
            label = "Shorts",
            isSelected = selectedTab == YouTubeNavTab.SHORTS,
            onClick = { onTabSelected(YouTubeNavTab.SHORTS) }
        )

        // 3. Nút Tạo (+) ở chính giữa
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF272727))
                .clickable { onTabSelected(YouTubeNavTab.CREATE) }
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Tạo / Thêm",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // 4. Kênh đăng ký
        NavItem(
            icon = Icons.Filled.Subscriptions,
            label = "Kênh đăng ký",
            isSelected = selectedTab == YouTubeNavTab.SUBSCRIPTIONS,
            onClick = { onTabSelected(YouTubeNavTab.SUBSCRIPTIONS) }
        )

        // 5. Bạn (Trang cá nhân / Thư viện)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clickable { onTabSelected(YouTubeNavTab.YOU) }
                .padding(vertical = 4.dp)
        ) {
            if (!userAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = userAvatarUrl,
                    contentDescription = "Bạn",
                    modifier = Modifier
                        .size(23.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Bạn",
                    tint = if (selectedTab == YouTubeNavTab.YOU) Color.White else TextMuted,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Bạn",
                fontSize = 10.sp,
                color = if (selectedTab == YouTubeNavTab.YOU) Color.White else TextMuted,
                fontWeight = if (selectedTab == YouTubeNavTab.YOU) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else TextMuted,
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) Color.White else TextMuted,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
