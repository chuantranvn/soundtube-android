package com.soundtube.app.ui.theme

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Hiệu ứng Quantum Neumorphism (Trắng Đục Lượng Tử - Soft UI).
 *
 * - pressed = false: khối nổi trắng đục ngọc trai, tích hợp ánh hào quang khúc xạ lượng tử nhẹ
 *   (iridescent quantum sheen) và viền lăng kính (prismatic rim) Hồng – Tím – Cyan.
 * - pressed = true : giếng thế năng lượng tử lõm xuống (quantum potential well) với viền phát sáng.
 */
fun Modifier.neumorphic(
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    quantumGlow: Boolean = false,
    borderWidth: Dp? = null,
    color: Color = NeuBase,
    lightShadow: Color = NeuLightShadow,
    darkShadow: Color = NeuDarkShadow
): Modifier = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply { addOutline(outline) }
    val offsetPx = elevation.toPx()
    val blurPx = (offsetPx * 1.85f).coerceAtLeast(1f)
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun shadowPaint(shadowColor: Color) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = shadowColor.toArgb()
        maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
    }

    val lightPaint = shadowPaint(lightShadow)
    val darkPaint = shadowPaint(darkShadow)
    val quantumShadowPaint = shadowPaint(
        if (quantumGlow) AccentPurple.copy(alpha = 0.28f) else darkShadow
    )

    // Vành rỗng bao quanh hình (để đổ bóng vào bên trong khi ở trạng thái lõm)
    val pad = offsetPx * 4f
    val ringPath = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(androidx.compose.ui.geometry.Rect(-pad, -pad, size.width + pad, size.height + pad))
        addOutline(outline)
    }

    // Bề mặt trắng ngọc trai pha sắc Hồng Phấn - Tím Oải Hương lượng tử
    val raisedSurfaceBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFEFAFD),
            color,
            Color(0xFFEBE2F6)
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )

    // Lớp phủ ánh sáng giao thoa lượng tử cực mịn trên bề mặt khối nổi (Hồng -> Tím, không dùng Cyan)
    val iridescentSheenBrush = Brush.linearGradient(
        colors = listOf(
            AccentRed.copy(alpha = if (quantumGlow) 0.08f else 0.03f),
            QuantumMagenta.copy(alpha = if (quantumGlow) 0.07f else 0.025f),
            AccentPurple.copy(alpha = if (quantumGlow) 0.08f else 0.03f)
        ),
        start = Offset(0f, size.height),
        end = Offset(size.width, 0f)
    )

    // Bề mặt giếng năng lượng lõm (pressed well)
    val pressedSurfaceBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE3DAF0),
            Color(0xFFEEE7F8),
            Color(0xFFFAF6FE)
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )

    onDrawWithContent {
        if (!pressed) {
            if (supportsBlur) {
                drawIntoCanvas { canvas ->
                    translate(-offsetPx * 0.85f, -offsetPx * 0.85f) {
                        canvas.nativeCanvas.drawPath(path.asAndroidPath(), lightPaint)
                    }
                    translate(offsetPx, offsetPx) {
                        canvas.nativeCanvas.drawPath(
                            path.asAndroidPath(),
                            if (quantumGlow) quantumShadowPaint else darkPaint
                        )
                    }
                }
            }
            drawPath(path, brush = raisedSurfaceBrush)
            drawPath(path, brush = iridescentSheenBrush)

            // Viền khúc xạ lăng kính lượng tử (Quantum Prismatic Specular Rim: Trắng -> Hồng -> Tím)
            drawPath(
                path,
                brush = Brush.linearGradient(
                    if (quantumGlow) {
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            AccentRed.copy(alpha = 0.48f),
                            QuantumMagenta.copy(alpha = 0.42f),
                            AccentPurple.copy(alpha = 0.48f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.55f),
                            AccentRed.copy(alpha = 0.16f),
                            AccentPurple.copy(alpha = 0.20f)
                        )
                    },
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                style = Stroke(width = (borderWidth ?: if (quantumGlow) 1.15.dp else 0.95.dp).toPx())
            )
        } else {
            drawPath(path, brush = pressedSurfaceBrush)
            drawPath(path, brush = iridescentSheenBrush)
            if (supportsBlur) {
                clipPath(path) {
                    drawIntoCanvas { canvas ->
                        translate(offsetPx * 0.7f, offsetPx * 0.7f) {
                            canvas.nativeCanvas.drawPath(ringPath.asAndroidPath(), darkPaint)
                        }
                        translate(-offsetPx * 0.7f, -offsetPx * 0.7f) {
                            canvas.nativeCanvas.drawPath(ringPath.asAndroidPath(), lightPaint)
                        }
                    }
                }
            } else {
                drawPath(path, Color(0x18000000))
            }
            // Viền năng lượng lượng tử mảnh tinh tế cho ô đang được chọn / nhấn (Hồng -> Tím)
            drawPath(
                path,
                brush = Brush.linearGradient(
                    listOf(
                        AccentRed.copy(alpha = 0.40f),
                        QuantumMagenta.copy(alpha = 0.35f),
                        AccentPurple.copy(alpha = 0.40f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                style = Stroke(width = (borderWidth ?: 0.8.dp).toPx())
            )
        }
        drawContent()
    }
}

/**
 * Nút neumorphic lượng tử: nổi khi bình thường, lõm xuống kèm viền lăng kính khi nhấn hoặc [selected].
 */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    elevation: Dp = 4.dp,
    selected: Boolean = false,
    quantumGlow: Boolean = false,
    color: Color = NeuBase,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .neumorphic(
                shape = shape,
                elevation = elevation,
                pressed = isPressed || selected,
                quantumGlow = quantumGlow || selected,
                color = color
            )
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        content()
    }
}

/** Nút tròn neumorphic lượng tử kích thước cố định. */
@Composable
fun NeuIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    elevation: Dp = 4.dp,
    selected: Boolean = false,
    quantumGlow: Boolean = false,
    content: @Composable () -> Unit
) {
    NeuButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        elevation = elevation,
        selected = selected,
        quantumGlow = quantumGlow,
        content = content
    )
}
