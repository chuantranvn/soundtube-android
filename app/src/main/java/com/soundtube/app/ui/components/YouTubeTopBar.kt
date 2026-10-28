package com.soundtube.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.soundtube.app.R
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.NeuIconButton
import com.soundtube.app.ui.theme.TextSecondary

@Composable
fun YouTubeTopBar(
    onSearchClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onLogoClick: () -> Unit = {},
    isSearchActive: Boolean = false,
    userAvatarUrl: String? = null,
    isLoggedIn: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Hiệu ứng sóng âm Equalizer (|||) nhấp nhô mượt mà ngay trước chữ "Tube" trong cụm Logo
    val infiniteTransition = rememberInfiniteTransition(label = "quarktube_logo_eq")
    val eqBar1 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 37f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqBar1"
    )
    val eqBar2 by infiniteTransition.animateFloat(
        initialValue = 39f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(110)
        ),
        label = "eqBar2"
    )
    val eqBar3 by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(230)
        ),
        label = "eqBar3"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Cụm Logo QuarkTube: bấm vào trả về Home + hiệu ứng Equalizer (|||) nhấp nhô trước chữ Tube
        Image(
            painter = painterResource(id = R.drawable.img_quarktube_logo),
            contentDescription = "QuarkTube Logo - Về trang chủ",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(42.dp)
                .aspectRatio(720f / 168f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onLogoClick
                )
                .drawWithContent {
                    drawContent()
                    // Hệ tọa độ chuẩn 720x168 của img_quarktube_logo.png:
                    // Viên nang Tube bắt đầu tại x=412, y=39 (cao 90 -> tâm dọc y=84)
                    // Ba thanh sóng âm nằm tại x = 438, 448, 458 (rộng 5.5)
                    val scaleX = size.width / 720f
                    val scaleY = size.height / 168f
                    val barW = 5.6f * scaleX
                    val radius = CornerRadius(barW / 2f, barW / 2f)
                    val centerY = 84f * scaleY

                    val bars = listOf(
                        Triple(438f, eqBar1, 0.90f),
                        Triple(448f, eqBar2, 1.00f),
                        Triple(458f, eqBar3, 0.93f)
                    )
                    bars.forEach { (svgX, svgH, alpha) ->
                        val barH = svgH * scaleY
                        val left = svgX * scaleX
                        val top = centerY - barH / 2f
                        drawRoundRect(
                            color = Color.White.copy(alpha = alpha),
                            topLeft = Offset(left, top),
                            size = Size(barW, barH),
                            cornerRadius = radius
                        )
                    }
                }
        )

        // Cụm nút bên phải: Tìm kiếm, Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuIconButton(
                onClick = onSearchClick,
                size = 40.dp,
                elevation = 3.dp,
                selected = isSearchActive,
                quantumGlow = isSearchActive
            ) {
                QuantumSearchIcon(size = 21.dp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Avatar tài khoản (viền neumorphic nổi)
            NeuIconButton(
                onClick = onAvatarClick,
                size = 38.dp,
                elevation = 3.dp
            ) {
                if (!userAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userAvatarUrl,
                        contentDescription = "Tài khoản",
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Tài khoản",
                        tint = if (isLoggedIn) AccentRed else TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
