package com.soundtube.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.soundtube.app.ui.theme.AccentGradient
import com.soundtube.app.ui.theme.AccentPurple
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.NeuIconButton
import com.soundtube.app.ui.theme.TextMuted
import com.soundtube.app.ui.theme.neumorphic

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
    // Thanh điều hướng nổi dạng neumorphic lượng tử (bo tròn, viền lăng kính, tách khỏi mép màn hình)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 10.dp)
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .neumorphic(shape = RoundedCornerShape(26.dp), elevation = 6.dp, borderWidth = 0.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Trang chủ (Icon Ngôi nhà Lượng tử)
        NavItemContainer(
            label = "Trang chủ",
            isSelected = selectedTab == YouTubeNavTab.HOME,
            onClick = { onTabSelected(YouTubeNavTab.HOME) }
        ) {
            QuantumHomeIcon(
                isSelected = selectedTab == YouTubeNavTab.HOME,
                size = 23.dp
            )
        }

        // 2. Shorts (Icon Xung năng lượng Lượng tử)
        NavItemContainer(
            label = "Shorts",
            isSelected = selectedTab == YouTubeNavTab.SHORTS,
            onClick = { onTabSelected(YouTubeNavTab.SHORTS) }
        ) {
            QuantumShortsIcon(
                isSelected = selectedTab == YouTubeNavTab.SHORTS,
                size = 23.dp
            )
        }

        // 3. Nút Tạo (+) ở chính giữa: đĩa neumorphic nổi + lõi lượng tử
        NeuIconButton(
            onClick = { onTabSelected(YouTubeNavTab.CREATE) },
            size = 46.dp,
            elevation = 4.dp,
            selected = selectedTab == YouTubeNavTab.CREATE
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentGradient)
            ) {
                QuantumPlusIcon(size = 22.dp)
            }
        }

        // 4. Kênh đăng ký (Icon Đa tầng Lượng tử)
        NavItemContainer(
            label = "Kênh đăng ký",
            isSelected = selectedTab == YouTubeNavTab.SUBSCRIPTIONS,
            onClick = { onTabSelected(YouTubeNavTab.SUBSCRIPTIONS) }
        ) {
            QuantumSubscriptionsIcon(
                isSelected = selectedTab == YouTubeNavTab.SUBSCRIPTIONS,
                size = 23.dp
            )
        }

        // 5. Bạn (Trang cá nhân / Thư viện)
        NavItemContainer(
            label = "Bạn",
            isSelected = selectedTab == YouTubeNavTab.YOU,
            onClick = { onTabSelected(YouTubeNavTab.YOU) }
        ) {
            if (!userAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = userAvatarUrl,
                    contentDescription = "Bạn",
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Bạn",
                    tint = if (selectedTab == YouTubeNavTab.YOU) AccentRed else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
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
    NavItemContainer(label = label, isSelected = isSelected, onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) AccentRed else TextMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Mục điều hướng: khi được chọn sẽ có hiệu ứng Neumorphic 3D lõm xuống (hoặc nổi khối mềm)
 * bằng bóng sáng/tối thuần túy, hoàn toàn KHÔNG vẽ đường viền (borderWidth = 0.dp).
 */
@Composable
private fun NavItemContainer(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier.neumorphic(
                        shape = shape,
                        elevation = 3.5.dp,
                        pressed = true,
                        borderWidth = 0.dp
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (isSelected) AccentRed else TextMuted,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
