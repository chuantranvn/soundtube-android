package com.soundtube.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ===== Bảng màu Quantum Frosted Neumorphic (Hỗ trợ cả Sáng & Tối Lượng Tử) =====

// Màu nền Neumorphic động
val NeuBase: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF141122) else Color(0xFFEDF0FA)

val NeuLightShadow: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF282240) else Color(0xFFFFFFFF)

val NeuDarkShadow: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF08070F) else Color(0xFFC2CCE0)

// Các dải màu Gradient màn hình từ Tím (Top) chuyển mượt sang Xanh Cyan (Bottom)
val BgGradientTop: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF19122D) else Color(0xFFF1E6FF)

val BgGradientUpperMid: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF141326) else Color(0xFFEAE8FC)

val BgGradientLowerMid: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF0E1A27) else Color(0xFFE0F3FC)

val BgGradientBottom: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF07141E) else Color(0xFFD2F4FA)

val DarkBackground: Color
    @Composable get() = NeuBase

val DarkSurface: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF1B172E) else Color(0xFFF5F8FD)

val DarkSurfaceVariant: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF241F3C) else Color(0xFFE2ECF7)

// Bộ màu Hạt Quark & Trường Spin Lượng Tử (giữ rực rỡ ở cả hai chế độ)
val AccentRed = Color(0xFFFF2E63)       // Hồng năng lượng Quark
val AccentRedLight = Color(0xFFFF6B93)
val AccentPurple = Color(0xFF7C4DFF)    // Tím trường lượng tử Spin
val QuantumMagenta = Color(0xFFD838B8)  // Tím hồng giao thoa sóng
val QuantumViolet = Color(0xFFA855F7)   // Tím thạch anh lượng tử
val QuantumCyan = Color(0xFF00C2FF)     // Xanh Cyan bức xạ lượng tử

// Màu chữ động theo chế độ Sáng / Tối
val TextPrimary: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFFF4F3F8) else Color(0xFF1A1528)

val TextSecondary: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFFA59EB8) else Color(0xFF5A5072)

val TextMuted: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xFF726A87) else Color(0xFF8C82A4)

val GlassBorder: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0x33FFFFFF) else Color(0xCCFFFFFF)

val FrostedSurface: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0xD9161324) else Color(0xE6FFFFFF)

val AccentSoftBg: Color
    @Composable get() = if (LocalThemeIsDark.current) Color(0x33FF2E63) else Color(0xFFFCE8F3)

// Gradient chính Hồng Quark -> Tím Hồng -> Tím Spin
val AccentGradient = Brush.linearGradient(
    listOf(AccentRed, QuantumMagenta, AccentPurple)
)

// Gradient Quang Phổ Lượng Tử
val QuantumSpectrumBrush = Brush.linearGradient(
    listOf(AccentRed, QuantumMagenta, AccentPurple, QuantumViolet)
)

// Gradient viền khúc xạ lăng kính cho các khối Neumorphic
val QuantumPrismBorderBrush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.95f),
        AccentRed.copy(alpha = 0.32f),
        QuantumMagenta.copy(alpha = 0.28f),
        AccentPurple.copy(alpha = 0.32f),
        Color.White.copy(alpha = 0.72f)
    )
)
