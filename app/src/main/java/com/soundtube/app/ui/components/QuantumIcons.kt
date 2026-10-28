package com.soundtube.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soundtube.app.ui.theme.AccentPurple
import com.soundtube.app.ui.theme.AccentRed
import com.soundtube.app.ui.theme.TextMuted
import com.soundtube.app.ui.theme.TextPrimary

/**
 * Bộ Icon Lượng Tử (Quantum Vector Icons) thiết kế riêng cho QuarkTube.
 * Kết hợp nét bo hình học hiện đại, vòng quỹ đạo elip nghiêng và hạt lượng tử (quark node) phát sáng.
 */
private fun quantumBrush(active: Boolean): Brush =
    if (active) {
        Brush.linearGradient(
            colors = listOf(AccentRed, Color(0xFFD838B8), AccentPurple)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(TextMuted, Color(0xFF6E7B91))
        )
    }

private fun quantumPrimaryBrush(): Brush =
    Brush.linearGradient(
        colors = listOf(TextPrimary, Color(0xFF374357))
    )

/**
 * 1. Icon Trang chủ Lượng Tử (Quantum Home):
 * Ngôi nhà bo góc hiện đại với lõi hạt nhân phát sáng và vòng quỹ đạo bao quanh mái.
 */
@Composable
fun QuantumHomeIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val brush = quantumBrush(isSelected)
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.088f

        // Vòng quỹ đạo lượng tử nghiêng nhẹ phía sau ngôi nhà
        rotate(degrees = -22f, pivot = Offset(w * 0.5f, h * 0.55f)) {
            drawOval(
                brush = brush,
                topLeft = Offset(w * 0.06f, h * 0.36f),
                size = Size(w * 0.88f, h * 0.36f),
                alpha = if (isSelected) 0.55f else 0.28f,
                style = Stroke(width = stroke * 0.65f, cap = StrokeCap.Round)
            )
        }

        // Thân ngôi nhà hình học bo tròn
        val housePath = Path().apply {
            moveTo(w * 0.18f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.48f)
            lineTo(w * 0.82f, h * 0.84f)
            lineTo(w * 0.18f, h * 0.84f)
            close()
        }
        if (isSelected) {
            drawPath(
                path = housePath,
                brush = brush,
                alpha = 0.16f
            )
        }
        drawPath(
            path = housePath,
            brush = brush,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Lõi hạt lượng tử phát sáng ở tâm nhà
        drawCircle(
            brush = brush,
            radius = w * 0.11f,
            center = Offset(w * 0.5f, h * 0.58f)
        )
        // Hạt vệ tinh nhỏ trên đỉnh mái
        drawCircle(
            color = if (isSelected) AccentRed else TextMuted,
            radius = w * 0.055f,
            center = Offset(w * 0.86f, h * 0.32f)
        )
    }
}

/**
 * 2. Icon Shorts Lượng Tử (Quantum Energy Pulse):
 * Tia năng lượng kép đặt trong vòng quỹ đạo spin lượng tử.
 */
@Composable
fun QuantumShortsIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val brush = quantumBrush(isSelected)
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        // Quỹ đạo elip nghiêng
        rotate(degrees = -30f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            drawOval(
                brush = brush,
                topLeft = Offset(w * 0.08f, h * 0.30f),
                size = Size(w * 0.84f, h * 0.40f),
                alpha = if (isSelected) 0.6f else 0.32f,
                style = Stroke(width = stroke * 0.7f)
            )
        }

        // Tia chớp lượng tử ở trung tâm
        val bolt = Path().apply {
            moveTo(w * 0.56f, h * 0.12f)
            lineTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.88f)
            lineTo(w * 0.72f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.48f)
            close()
        }
        if (isSelected) {
            drawPath(path = bolt, brush = brush)
        } else {
            drawPath(
                path = bolt,
                brush = brush,
                style = Stroke(width = stroke * 0.9f, join = StrokeJoin.Round)
            )
        }

        // 2 hạt Quark đối xứng
        drawCircle(
            color = if (isSelected) AccentRed else TextMuted,
            radius = w * 0.055f,
            center = Offset(w * 0.85f, h * 0.28f)
        )
        drawCircle(
            color = if (isSelected) AccentPurple else TextMuted,
            radius = w * 0.048f,
            center = Offset(w * 0.15f, h * 0.72f)
        )
    }
}

/**
 * 3. Icon Kênh đăng ký Lượng Tử (Quantum Subscriptions):
 * Khối phát sóng đa tầng kèm quỹ đạo vệ tinh và nút Play ở tâm.
 */
@Composable
fun QuantumSubscriptionsIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val brush = quantumBrush(isSelected)
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f

        // 2 vạch sóng năng lượng phía trên
        drawLine(
            brush = brush,
            start = Offset(w * 0.30f, h * 0.16f),
            end = Offset(w * 0.70f, h * 0.16f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = brush,
            start = Offset(w * 0.22f, h * 0.27f),
            end = Offset(w * 0.78f, h * 0.27f),
            strokeWidth = stroke * 0.85f,
            cap = StrokeCap.Round
        )

        // Khung màn hình bo tròn
        drawRoundRect(
            brush = brush,
            topLeft = Offset(w * 0.14f, h * 0.38f),
            size = Size(w * 0.72f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
            style = Stroke(width = stroke)
        )

        // Nút Play nhỏ ở tâm
        val play = Path().apply {
            moveTo(w * 0.44f, h * 0.52f)
            lineTo(w * 0.44f, h * 0.72f)
            lineTo(w * 0.62f, h * 0.62f)
            close()
        }
        drawPath(path = play, brush = brush)

        // Hạt lượng tử ở góc trên phải
        drawCircle(
            color = if (isSelected) AccentRed else TextMuted,
            radius = w * 0.055f,
            center = Offset(w * 0.86f, h * 0.20f)
        )
    }
}

/**
 * 4. Icon Tìm kiếm Lượng Tử (Quantum Search):
 * Thấu kính có vòng quỹ đạo nghiêng và hạt Quark xoay quanh.
 */
@Composable
fun QuantumSearchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 21.dp
) {
    val accentBrush = quantumBrush(true)
    val primaryBrush = quantumPrimaryBrush()
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.095f
        val center = Offset(w * 0.43f, h * 0.43f)
        val radius = w * 0.27f

        // Vòng quỹ đạo elip nghiêng ôm lấy kính lúp
        rotate(degrees = -28f, pivot = center) {
            drawOval(
                brush = accentBrush,
                topLeft = Offset(center.x - radius * 1.38f, center.y - radius * 0.55f),
                size = Size(radius * 2.76f, radius * 1.1f),
                alpha = 0.65f,
                style = Stroke(width = stroke * 0.62f)
            )
        }

        // Vòng tròn kính lúp chính
        drawCircle(
            brush = primaryBrush,
            radius = radius,
            center = center,
            style = Stroke(width = stroke)
        )

        // Tay cầm kính lúp
        drawLine(
            brush = accentBrush,
            start = Offset(w * 0.63f, h * 0.63f),
            end = Offset(w * 0.86f, h * 0.86f),
            strokeWidth = stroke * 1.15f,
            cap = StrokeCap.Round
        )

        // Hạt lượng tử phát sáng ở tâm thấu kính
        drawCircle(
            brush = accentBrush,
            radius = w * 0.065f,
            center = center
        )
    }
}

/**
 * 5. Icon Khám phá Lượng Tử (Quantum Atom / Gyroscope):
 * Nguyên tử 2 quỹ đạo giao nhau với hạt nhân đỏ hồng ở tâm.
 */
@Composable
fun QuantumExploreIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val brush = quantumBrush(true)
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val stroke = w * 0.082f

        // Quỹ đạo 1 (+35 độ)
        rotate(degrees = 35f, pivot = center) {
            drawOval(
                brush = brush,
                topLeft = Offset(w * 0.08f, h * 0.30f),
                size = Size(w * 0.84f, h * 0.40f),
                style = Stroke(width = stroke)
            )
        }
        // Quỹ đạo 2 (-35 độ)
        rotate(degrees = -35f, pivot = center) {
            drawOval(
                brush = brush,
                topLeft = Offset(w * 0.08f, h * 0.30f),
                size = Size(w * 0.84f, h * 0.40f),
                style = Stroke(width = stroke)
            )
        }

        // Hạt nhân Quark ở tâm
        drawCircle(
            brush = brush,
            radius = w * 0.13f,
            center = center
        )
    }
}

/**
 * 6. Icon Dấu Cộng Lượng Tử (Quantum Core Plus):
 * Dấu + phát sáng đặt giữa 2 vòng cung quỹ đạo.
 */
@Composable
fun QuantumPlusIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val stroke = w * 0.105f

        // Vòng quỹ đạo mảnh màu trắng bán trong suốt
        rotate(degrees = -25f, pivot = center) {
            drawOval(
                color = Color.White.copy(alpha = 0.42f),
                topLeft = Offset(w * 0.08f, h * 0.32f),
                size = Size(w * 0.84f, h * 0.36f),
                style = Stroke(width = stroke * 0.52f)
            )
        }

        // Dấu + chính
        drawLine(
            color = Color.White,
            start = Offset(w * 0.5f, h * 0.24f),
            end = Offset(w * 0.5f, h * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.24f, h * 0.5f),
            end = Offset(w * 0.76f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // Hạt vệ tinh nhỏ
        drawCircle(
            color = Color.White,
            radius = w * 0.055f,
            center = Offset(w * 0.82f, h * 0.28f)
        )
    }
}

/**
 * 7. Vòng hào quang quỹ đạo lượng tử (Quantum Orbital Halo)
 * Dùng để vẽ bên ngoài các nút phát nhạc hoặc avatar để tạo chất lượng tử đồng bộ.
 */
@Composable
fun QuantumOrbitalHalo(
    modifier: Modifier = Modifier,
    size: Dp = 84.dp
) {
    val brush = quantumBrush(true)
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w * 0.5f, h * 0.5f)

        rotate(degrees = -24f, pivot = center) {
            drawOval(
                brush = brush,
                topLeft = Offset(w * 0.02f, h * 0.24f),
                size = Size(w * 0.96f, h * 0.52f),
                alpha = 0.45f,
                style = Stroke(width = 1.6.dp.toPx())
            )
            // Hạt Quark 1
            drawCircle(
                color = AccentRed,
                radius = 3.5.dp.toPx(),
                center = Offset(w * 0.98f, h * 0.5f)
            )
            // Hạt Quark 2
            drawCircle(
                color = AccentPurple,
                radius = 2.8.dp.toPx(),
                center = Offset(w * 0.02f, h * 0.5f)
            )
        }
    }
}

