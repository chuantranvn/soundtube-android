package com.soundtube.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundtube.app.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lớp nền Trường Lượng Tử (Quantum Field Canvas):
 * - Phủ lớp Gradient dọc từ trên xuống dưới (Top-to-Bottom: Trắng Hồng Phấn -> Ngọc Trai Oải Hương -> Tím Sương Mù)
 *   dịu mắt, có chiều sâu Neumorphic, thay thế hoàn toàn nền trắng đơn điệu.
 * - Các vùng cực quang lượng tử Hồng Quark & Tím Spin (không dùng Cyan) cùng quỹ đạo hạt tinh tế.
 */
@Composable
fun QuantumFieldBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 0. Lớp nền Gradient nhẹ từ trên xuống dưới (Top -> Bottom) rất dịu mắt
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    BgGradientTop,
                    BgGradientUpperMid,
                    BgGradientLowerMid,
                    BgGradientBottom
                ),
                startY = 0f,
                endY = h
            ),
            size = size
        )

        // 1. Cực quang hạt Quark Hồng ở góc trên trái (dưới logo)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AccentRed.copy(alpha = 0.11f),
                    QuantumMagenta.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(w * 0.18f, h * 0.08f),
                radius = w * 0.58f
            ),
            radius = w * 0.58f,
            center = Offset(w * 0.18f, h * 0.08f)
        )

        // 2. Cực quang Tím Hồng Giao Thoa ở cạnh phải giữa màn hình
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    QuantumMagenta.copy(alpha = 0.10f),
                    AccentPurple.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = Offset(w * 0.88f, h * 0.42f),
                radius = w * 0.62f
            ),
            radius = w * 0.62f,
            center = Offset(w * 0.88f, h * 0.42f)
        )

        // 3. Cực quang Tím Spin ở góc dưới trái (gần thanh điều hướng)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AccentPurple.copy(alpha = 0.12f),
                    AccentRed.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(w * 0.25f, h * 0.88f),
                radius = w * 0.60f
            ),
            radius = w * 0.60f,
            center = Offset(w * 0.25f, h * 0.88f)
        )

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 18f), 0f)
        val orbitBrush = Brush.linearGradient(
            colors = listOf(
                AccentRed.copy(alpha = 0.16f),
                QuantumMagenta.copy(alpha = 0.14f),
                AccentPurple.copy(alpha = 0.18f)
            )
        )

        // 4. Vòng quỹ đạo trường lượng tử lớn phía trên
        rotate(degrees = -22f, pivot = Offset(w * 0.52f, h * 0.14f)) {
            drawOval(
                brush = orbitBrush,
                topLeft = Offset(-w * 0.10f, h * 0.04f),
                size = Size(w * 1.24f, h * 0.20f),
                style = Stroke(width = 1.1.dp.toPx(), pathEffect = dashEffect)
            )
        }

        // 5. Vòng quỹ đạo trường lượng tử giao thoa phía dưới
        rotate(degrees = 26f, pivot = Offset(w * 0.50f, h * 0.78f)) {
            drawOval(
                brush = orbitBrush,
                topLeft = Offset(-w * 0.12f, h * 0.66f),
                size = Size(w * 1.24f, h * 0.22f),
                style = Stroke(width = 1.1.dp.toPx(), pathEffect = dashEffect)
            )
        }

        // 6. Các hạt Quark (Quantum Nodes) lơ lửng tinh tế trên lưới quỹ đạo
        val nodes = listOf(
            Triple(Offset(w * 0.86f, h * 0.07f), AccentRed, 3.2.dp.toPx()),
            Triple(Offset(w * 0.12f, h * 0.22f), QuantumMagenta, 2.8.dp.toPx()),
            Triple(Offset(w * 0.91f, h * 0.58f), AccentPurple, 3.0.dp.toPx()),
            Triple(Offset(w * 0.09f, h * 0.74f), AccentRed, 2.6.dp.toPx())
        )
        nodes.forEach { (pos, color, r) ->
            drawCircle(
                color = color.copy(alpha = 0.16f),
                radius = r * 2.8f,
                center = pos
            )
            drawCircle(
                color = color.copy(alpha = 0.52f),
                radius = r,
                center = pos
            )
        }
    }
}

/**
 * Bộ Loading Lượng Tử 3D (Quantum Orbital Atom Loader):
 * Thay thế hoàn toàn CircularProgressIndicator mặc định.
 * Gồm đĩa Neumorphic nổi ở tâm, 3 quỹ đạo elip xoay ngược chiều tốc độ cao kèm hạt Quark phát sáng
 * và sóng hàm Schrödinger dao động bên dưới.
 */
@Composable
fun QuantumLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Đang đồng bộ trường âm thanh lượng tử..."
) {
    val transition = rememberInfiniteTransition(label = "quantum_loader")
    val orbitAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )
    val reverseAngle by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverseAngle"
    )
    val corePulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Đĩa Lõi Lượng Tử Neumorphic
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(92.dp)
                .neumorphic(
                    shape = CircleShape,
                    elevation = 6.dp,
                    quantumGlow = true
                )
        ) {
            Canvas(modifier = Modifier.size(76.dp)) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)
                val rx = w * 0.44f
                val ry = h * 0.19f

                // Quầng sáng năng lượng ở tâm
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentRed.copy(alpha = 0.25f * corePulse),
                            AccentPurple.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = w * 0.36f
                    ),
                    radius = w * 0.36f,
                    center = center
                )

                val orbitGrad = Brush.linearGradient(
                    colors = listOf(AccentRed, QuantumMagenta, AccentPurple)
                )

                // 3 quỹ đạo nguyên tử giao thoa xoay 3 chiều
                val tilts = listOf(
                    Triple(-30f + orbitAngle * 0.35f, orbitAngle, AccentRed),
                    Triple(30f - orbitAngle * 0.25f, reverseAngle, QuantumMagenta),
                    Triple(90f + orbitAngle * 0.20f, orbitAngle + 120f, AccentPurple)
                )

                tilts.forEach { (tiltDeg, particleDeg, quarkColor) ->
                    rotate(degrees = tiltDeg, pivot = center) {
                        drawOval(
                            brush = orbitGrad,
                            topLeft = Offset(center.x - rx, center.y - ry),
                            size = Size(rx * 2f, ry * 2f),
                            alpha = 0.68f,
                            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        val rad = Math.toRadians(particleDeg.toDouble())
                        val px = center.x + (rx * cos(rad)).toFloat()
                        val py = center.y + (ry * sin(rad)).toFloat()
                        // Quầng hào quang hạt Quark
                        drawCircle(
                            color = quarkColor.copy(alpha = 0.35f),
                            radius = 6.dp.toPx(),
                            center = Offset(px, py)
                        )
                        // Nhân hạt Quark
                        drawCircle(
                            color = quarkColor,
                            radius = 3.2.dp.toPx(),
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.4.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }

                // Hạt nhân Singularity nhịp đập ở chính tâm
                drawCircle(
                    brush = Brush.linearGradient(listOf(AccentRed, AccentPurple)),
                    radius = 7.5.dp.toPx() * corePulse,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 2.6.dp.toPx(),
                    center = Offset(center.x - 1.5.dp.toPx(), center.y - 1.5.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sóng giao thoa lượng tử động ngay dưới đĩa
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .height(14.dp)
        ) {
            val w = size.width
            val cy = size.height / 2f
            val amp = size.height * 0.36f
            val path1 = Path()
            val path2 = Path()
            val steps = 48
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                val x = t * w
                val env = sin(t * Math.PI).toFloat()
                val y1 = cy + sin(t * Math.PI * 4.0 + wavePhase).toFloat() * amp * env
                val y2 = cy - sin(t * Math.PI * 4.0 - wavePhase + 0.8).toFloat() * amp * 0.75f * env
                if (i == 0) {
                    path1.moveTo(x, y1)
                    path2.moveTo(x, y2)
                } else {
                    path1.lineTo(x, y1)
                    path2.lineTo(x, y2)
                }
            }
            drawPath(
                path = path2,
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, AccentPurple.copy(alpha = 0.55f), QuantumMagenta.copy(alpha = 0.55f), Color.Transparent)
                ),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = path1,
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, AccentRed.copy(alpha = 0.85f), AccentPurple.copy(alpha = 0.85f), Color.Transparent)
                ),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Bộ Loading Lượng Tử thu gọn (dùng khi cuộn tải thêm ở cuối danh sách).
 */
@Composable
fun QuantumMiniLoader(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "quantum_mini_loader")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miniAngle"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .neumorphic(shape = CircleShape, elevation = 3.dp, quantumGlow = true)
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val rx = size.width * 0.42f
            val ry = size.height * 0.20f
            val brush = Brush.linearGradient(listOf(AccentRed, QuantumMagenta, AccentPurple))
            rotate(-28f + angle * 0.5f, pivot = c) {
                drawOval(
                    brush = brush,
                    topLeft = Offset(c.x - rx, c.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
            rotate(32f - angle * 0.5f, pivot = c) {
                drawOval(
                    brush = brush,
                    topLeft = Offset(c.x - rx, c.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
            drawCircle(brush = brush, radius = 3.5.dp.toPx(), center = c)
        }
    }
}

/**
 * Màn hình Khởi động Lượng Tử (Quantum Animated Splash Screen):
 * - Hiệu ứng Lõi Phản Ứng Quark 3D Neumorphic với 3 vòng quỹ đạo hạt xoay tốc độ cao & sóng xung kích lan tỏa.
 * - Cụm Logo QuarkTube kèm sóng Equalizer nhấp nhô sống động.
 * - Slogan Lượng Tử cực ngầu mang sắc Hồng Quark -> Tím Spin (không dùng Cyan).
 */
@Composable
fun QuantumSplashScreen(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "quantum_splash")

    val orbitSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashOrbitSpin"
    )
    val counterSpin by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashCounterSpin"
    )
    val shockwave by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashShockwave"
    )
    val coreScale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashCoreScale"
    )
    val eq1 by transition.animateFloat(
        initialValue = 13f,
        targetValue = 38f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashEq1"
    )
    val eq2 by transition.animateFloat(
        initialValue = 39f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 340, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(100)
        ),
        label = "splashEq2"
    )
    val eq3 by transition.animateFloat(
        initialValue = 15f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(210)
        ),
        label = "splashEq3"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BgGradientTop,
                        BgGradientUpperMid,
                        BgGradientLowerMid,
                        BgGradientBottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Lớp nền trường lượng tử + vòng sóng xung kích lan tỏa
        QuantumFieldBackground()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.39f)
            val baseRadius = size.width * 0.26f
            val waveAlpha = (1.45f - shockwave).coerceIn(0f, 1f) * 0.35f

            drawCircle(
                brush = Brush.linearGradient(listOf(AccentRed, QuantumMagenta, AccentPurple)),
                radius = baseRadius * shockwave,
                center = center,
                alpha = waveAlpha,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AccentRed.copy(alpha = 0.14f),
                        AccentPurple.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.45f
                ),
                radius = baseRadius * 1.45f,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            // 1. Lõi Phản Ứng Lượng Tử 3D Neumorphic (Quantum Reactor Emblem)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(152.dp)
                    .scale(coreScale)
                    .neumorphic(
                        shape = CircleShape,
                        elevation = 10.dp,
                        quantumGlow = true
                    )
            ) {
                // Giếng thế năng lõm ở tâm đĩa
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(104.dp)
                        .neumorphic(
                            shape = CircleShape,
                            elevation = 4.dp,
                            pressed = true,
                            quantumGlow = true
                        )
                ) {}

                // Vẽ 3 quỹ đạo hạt Quark xoay tốc độ cao + Nút Play Lượng Tử ở tâm
                Canvas(modifier = Modifier.size(142.dp)) {
                    val w = size.width
                    val h = size.height
                    val c = Offset(w / 2f, h / 2f)
                    val rx = w * 0.45f
                    val ry = h * 0.18f
                    val accentGrad = Brush.linearGradient(
                        listOf(AccentRed, QuantumMagenta, AccentPurple)
                    )

                    val orbits = listOf(
                        Triple(-28f + orbitSpin * 0.25f, orbitSpin, AccentRed),
                        Triple(32f - orbitSpin * 0.20f, counterSpin, QuantumMagenta),
                        Triple(88f + orbitSpin * 0.15f, orbitSpin + 140f, AccentPurple)
                    )

                    orbits.forEach { (tilt, pAngle, pColor) ->
                        rotate(degrees = tilt, pivot = c) {
                            drawOval(
                                brush = accentGrad,
                                topLeft = Offset(c.x - rx, c.y - ry),
                                size = Size(rx * 2f, ry * 2f),
                                alpha = 0.78f,
                                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                            )
                            val rad = Math.toRadians(pAngle.toDouble())
                            val px = c.x + (rx * cos(rad)).toFloat()
                            val py = c.y + (ry * sin(rad)).toFloat()
                            drawCircle(
                                color = pColor.copy(alpha = 0.36f),
                                radius = 8.dp.toPx(),
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = pColor,
                                radius = 4.2.dp.toPx(),
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 1.8.dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }

                    // Tam giác Play Lượng Tử phát sáng ở tâm đĩa
                    val playPath = Path().apply {
                        moveTo(c.x - 11.dp.toPx(), c.y - 16.dp.toPx())
                        lineTo(c.x - 11.dp.toPx(), c.y + 16.dp.toPx())
                        lineTo(c.x + 17.dp.toPx(), c.y)
                        close()
                    }
                    drawPath(
                        path = playPath,
                        brush = accentGrad
                    )
                    drawPath(
                        path = playPath,
                        brush = accentGrad,
                        style = Stroke(width = 6.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Cụm Logo QuarkTube nguyên khối kèm sóng Equalizer nhấp nhô
            Image(
                painter = painterResource(id = R.drawable.img_quarktube_logo),
                contentDescription = "QuarkTube Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(58.dp)
                    .aspectRatio(720f / 168f)
                    .drawWithContent {
                        drawContent()
                        val scaleX = size.width / 720f
                        val scaleY = size.height / 168f
                        val barW = 5.6f * scaleX
                        val radius = CornerRadius(barW / 2f, barW / 2f)
                        val centerY = 84f * scaleY
                        val bars = listOf(
                            Triple(438f, eq1, 0.92f),
                            Triple(448f, eq2, 1.00f),
                            Triple(458f, eq3, 0.94f)
                        )
                        bars.forEach { (svgX, svgH, alpha) ->
                            val barH = svgH * scaleY
                            drawRoundRect(
                                color = Color.White.copy(alpha = alpha),
                                topLeft = Offset(svgX * scaleX, centerY - barH / 2f),
                                size = Size(barW, barH),
                                cornerRadius = radius
                            )
                        }
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Slogan Lượng Tử cực ngầu trong huy hiệu Neumorphic
            Box(
                modifier = Modifier
                    .neumorphic(
                        shape = RoundedCornerShape(20.dp),
                        elevation = 4.dp,
                        quantumGlow = true
                    )
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "⚛ ĐỒNG ĐIỆU TẦN SỐ • XUYÊN KHÔNG GIAN LƯỢNG TỬ ⚛",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentRed, QuantumMagenta, AccentPurple)
                        ),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Đánh thức mọi giác quan trên bước sóng đa vũ trụ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(34.dp))

            // 4. Sóng giao thoa lượng tử động báo trạng thái khởi tạo
            QuantumWaveDivider(
                modifier = Modifier.fillMaxWidth(0.55f),
                height = 18.dp
            )
        }
    }
}

/**
 * Thanh tiến trình Sóng Lượng Tử (Quantum Wave Progress Bar):
 * Vẽ đường tiến trình phát bằng dải quang phổ Hồng -> Tím Hồng -> Tím Spin kèm hạt photon phát sáng ở đầu mút.
 */
@Composable
fun QuantumWaveProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val cy = h / 2f

        // Rãnh nền lõm nhẹ
        drawLine(
            color = NeuDarkShadow.copy(alpha = 0.55f),
            start = Offset(0f, cy),
            end = Offset(w, cy),
            strokeWidth = h,
            cap = StrokeCap.Round
        )

        if (clamped > 0f) {
            val activeW = (w * clamped).coerceAtLeast(h)
            // Tia năng lượng quang phổ lượng tử (Hồng -> Tím, không Cyan)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(AccentRed, QuantumMagenta, AccentPurple, QuantumViolet),
                    startX = 0f,
                    endX = activeW
                ),
                start = Offset(0f, cy),
                end = Offset(activeW, cy),
                strokeWidth = h,
                cap = StrokeCap.Round
            )

            // Hạt Photon phát sáng tại đầu mút tiến trình
            val tip = Offset(activeW.coerceIn(h, w - h), cy)
            drawCircle(
                color = AccentRed.copy(alpha = 0.34f),
                radius = h * 1.9f,
                center = tip
            )
            drawCircle(
                color = Color.White,
                radius = h * 0.65f,
                center = tip
            )
        }
    }
}

/**
 * Bộ hiển thị Sóng Giao Thoa Lượng Tử (Quantum Wave Equalizer):
 * Vẽ 2 đường sóng hàm sin lệch pha (Hồng Quark & Tím Spin) kèm hạt nhân năng lượng.
 */
@Composable
fun QuantumWaveDivider(
    modifier: Modifier = Modifier,
    height: Dp = 18.dp
) {
    val transition = rememberInfiniteTransition(label = "wave_divider")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveDividerPhase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        val amp = h * 0.30f

        val wave1 = Path()
        val wave2 = Path()
        val steps = 60
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = t * w
            val env = sin(t * Math.PI).toFloat() // biên độ hội tụ ở hai đầu
            val y1 = cy + sin(t * Math.PI * 4.0 + phase).toFloat() * amp * env
            val y2 = cy - sin(t * Math.PI * 4.0 - phase + 0.9).toFloat() * amp * 0.8f * env
            if (i == 0) {
                wave1.moveTo(x, y1)
                wave2.moveTo(x, y2)
            } else {
                wave1.lineTo(x, y1)
                wave2.lineTo(x, y2)
            }
        }

        drawPath(
            path = wave2,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, QuantumMagenta.copy(alpha = 0.55f), AccentPurple.copy(alpha = 0.55f), Color.Transparent)
            ),
            style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = wave1,
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, AccentRed.copy(alpha = 0.75f), AccentPurple.copy(alpha = 0.75f), Color.Transparent)
            ),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        )

        // Hạt nhân lượng tử ở tâm giao thoa
        drawCircle(
            color = AccentRed.copy(alpha = 0.25f),
            radius = 5.dp.toPx(),
            center = Offset(w * 0.5f, cy)
        )
        drawCircle(
            color = AccentRed,
            radius = 2.2.dp.toPx(),
            center = Offset(w * 0.5f, cy)
        )
    }
}
