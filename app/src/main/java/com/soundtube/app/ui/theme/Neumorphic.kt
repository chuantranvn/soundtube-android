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
import androidx.compose.ui.composed
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
 * Hiệu ứng Quantum Neumorphism (Trắng Đục Lượng Tử & Tối Lượng Tử - Soft UI).
 * Tự động thích ứng chế độ Sáng / Tối:
 * - Chế độ Sáng: Bề mặt ngọc trai pha sắc Tím dịu, bóng đổ mềm tự nhiên.
 * - Chế độ Tối: Bề mặt đá Obsidian không gian vũ trụ, ánh sáng nổi và lòng giếng thế năng sâu thẳm.
 */
fun Modifier.neumorphic(
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 6.dp,
    pressed: Boolean = false,
    quantumGlow: Boolean = false,
    borderWidth: Dp? = null,
    color: Color = Color.Unspecified,
    lightShadow: Color = Color.Unspecified,
    darkShadow: Color = Color.Unspecified
): Modifier = composed {
    val isDark = LocalThemeIsDark.current
    val effectiveColor = if (color != Color.Unspecified) color else if (isDark) Color(0xFF161324) else Color(0xFFEDF0FA)
    val effectiveLightShadow = if (lightShadow != Color.Unspecified) lightShadow else if (isDark) Color(0xFF282342) else Color(0xFFFFFFFF)
    val effectiveDarkShadow = if (darkShadow != Color.Unspecified) darkShadow else if (isDark) Color(0xFF07060D) else Color(0xFFC2CCE0)

    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetPx = with(density) { elevation.toPx() }
    val blurPx = (offsetPx * 1.85f).coerceAtLeast(1f)
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    val lightPaint = remember(effectiveLightShadow, blurPx) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = effectiveLightShadow.toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val darkPaint = remember(effectiveDarkShadow, blurPx) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = effectiveDarkShadow.toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val quantumShadowPaint = remember(quantumGlow, isDark, effectiveDarkShadow, blurPx) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            val qColor = if (quantumGlow) AccentPurple.copy(alpha = if (isDark) 0.38f else 0.28f) else effectiveDarkShadow
            this.color = qColor.toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }
    val deepDarkPaint = remember(isDark, blurPx) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            val dColor = if (isDark) Color(0xFF050409) else Color(0xFFB3C1D8)
            this.color = dColor.toArgb()
            maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
    }

    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }

        // Vành rỗng bao quanh hình (để đổ bóng vào bên trong khi ở trạng thái lõm)
        val pad = offsetPx * 4f
        val ringPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(-pad, -pad, size.width + pad, size.height + pad))
            addOutline(outline)
        }

        // Bề mặt khối nổi 3D Neumorphic rõ nét
        val raisedSurfaceBrush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF221D38),
                    effectiveColor,
                    Color(0xFF131021)
                )
            } else {
                listOf(
                    Color(0xFFFFFFFF),
                    effectiveColor,
                    Color(0xFFE0E7F5)
                )
            },
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )

        // Lớp phủ ánh sáng giao thoa lượng tử cực mịn trên bề mặt
        val iridescentSheenBrush = Brush.linearGradient(
            colors = listOf(
                AccentRed.copy(alpha = if (quantumGlow) 0.08f else 0.02f),
                AccentPurple.copy(alpha = if (quantumGlow) 0.08f else 0.02f),
                QuantumCyan.copy(alpha = if (quantumGlow) 0.08f else 0.02f)
            ),
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f)
        )

        // Bề mặt giếng lõm 3D Neumorphic (độ tương phản lòng lõm rõ ràng, không cần viền)
        val pressedSurfaceBrush = Brush.linearGradient(
            colors = if (isDark) {
                listOf(
                    Color(0xFF0A0912),
                    Color(0xFF131020),
                    Color(0xFF1F1A33)
                )
            } else {
                listOf(
                    Color(0xFFD5DFEE),
                    Color(0xFFE6EDF8),
                    Color(0xFFFBFDFF)
                )
            },
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )

        onDrawWithContent {
            if (!pressed) {
                if (supportsBlur) {
                    drawIntoCanvas { canvas ->
                        translate(-offsetPx * 0.9f, -offsetPx * 0.9f) {
                            canvas.nativeCanvas.drawPath(path.asAndroidPath(), lightPaint)
                        }
                        translate(offsetPx * 1.05f, offsetPx * 1.05f) {
                            canvas.nativeCanvas.drawPath(
                                path.asAndroidPath(),
                                if (quantumGlow) quantumShadowPaint else deepDarkPaint
                            )
                        }
                    }
                }
                drawPath(path, brush = raisedSurfaceBrush)
                drawPath(path, brush = iridescentSheenBrush)

                // Chỉ vẽ viền khi borderWidth được truyền vào > 0.dp
                val raisedBorderDp = borderWidth ?: if (quantumGlow) 1.1.dp else 0.dp
                if (raisedBorderDp > 0.dp) {
                    drawPath(
                        path,
                        brush = Brush.linearGradient(
                            if (quantumGlow) {
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.5f else 0.95f),
                                    AccentRed.copy(alpha = 0.45f),
                                    AccentPurple.copy(alpha = 0.45f),
                                    QuantumCyan.copy(alpha = 0.45f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = if (isDark) 0.3f else 0.90f),
                                    AccentPurple.copy(alpha = 0.18f)
                                )
                            },
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        ),
                        style = Stroke(width = raisedBorderDp.toPx())
                    )
                }
            } else {
                drawPath(path, brush = pressedSurfaceBrush)
                drawPath(path, brush = iridescentSheenBrush)
                if (supportsBlur) {
                    clipPath(path) {
                        drawIntoCanvas { canvas ->
                            translate(offsetPx * 0.85f, offsetPx * 0.85f) {
                                canvas.nativeCanvas.drawPath(ringPath.asAndroidPath(), deepDarkPaint)
                            }
                            translate(-offsetPx * 0.85f, -offsetPx * 0.85f) {
                                canvas.nativeCanvas.drawPath(ringPath.asAndroidPath(), lightPaint)
                            }
                        }
                    }
                } else {
                    drawPath(path, Color(0x18000000))
                }
                // Chỉ vẽ viền khi borderWidth được truyền vào > 0.dp (mặc định 0.dp cho nút/icon lõm)
                val pressedBorderDp = borderWidth ?: 0.dp
                if (pressedBorderDp > 0.dp) {
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
                        style = Stroke(width = pressedBorderDp.toPx())
                    )
                }
            }
            drawContent()
        }
    }
}

/**
 * Nút neumorphic lượng tử: nổi 3D khi bình thường, lõm 3D xuống khi nhấn hoặc [selected] (mặc định không viền).
 */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    elevation: Dp = 4.dp,
    selected: Boolean = false,
    quantumGlow: Boolean = false,
    borderWidth: Dp? = 0.dp,
    color: Color = Color.Unspecified,
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
                borderWidth = borderWidth,
                color = color
            )
            .clip(shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        content()
    }
}

/** Nút tròn icon neumorphic nổi bật không viền. */
@Composable
fun NeuIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    elevation: Dp = 4.dp,
    selected: Boolean = false,
    quantumGlow: Boolean = false,
    borderWidth: Dp? = 0.dp,
    content: @Composable () -> Unit
) {
    NeuButton(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        elevation = elevation,
        selected = selected,
        quantumGlow = quantumGlow,
        borderWidth = borderWidth,
        content = content
    )
}
