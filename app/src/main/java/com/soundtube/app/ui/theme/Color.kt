package com.soundtube.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ===== Bảng màu Quantum Frosted Neumorphic (Nền Gradient Tím -> Xanh Cyan từ trên xuống dưới) =====
val NeuBase = Color(0xFFEDF0FA)
val NeuLightShadow = Color(0xFFFFFFFF)
val NeuDarkShadow = Color(0xFFC2CCE0)

// Các dải màu cho Gradient màn hình từ Tím (Top) chuyển mượt sang Xanh Cyan (Bottom)
val BgGradientTop = Color(0xFFF1E6FF)       // Đỉnh màn hình: Tím Lượng Tử ngọc trai dịu mắt
val BgGradientUpperMid = Color(0xFFEAE8FC)  // Giữa trên: Tím Oải Hương giao thoa
val BgGradientLowerMid = Color(0xFFE0F3FC)  // Giữa dưới: Xanh Băng Lượng Tử
val BgGradientBottom = Color(0xFFD2F4FA)    // Đáy màn hình: Xanh Cyan Cherenkov dịu mát

val DarkBackground = NeuBase
val DarkSurface = Color(0xFFF5F8FD)
val DarkSurfaceVariant = Color(0xFFE2ECF7)

// Bộ màu Hạt Quark & Trường Spin Lượng Tử
val AccentRed = Color(0xFFFF2E63)       // Hồng năng lượng Quark
val AccentRedLight = Color(0xFFFF6B93)
val AccentPurple = Color(0xFF7C4DFF)    // Tím trường lượng tử Spin
val QuantumMagenta = Color(0xFFD838B8)  // Tím hồng giao thoa sóng
val QuantumViolet = Color(0xFFA855F7)   // Tím thạch anh lượng tử
val QuantumCyan = Color(0xFF00C2FF)     // Xanh Cyan bức xạ lượng tử

val TextPrimary = Color(0xFF1A1528)
val TextSecondary = Color(0xFF5A5072)
val TextMuted = Color(0xFF8C82A4)
val GlassBorder = Color(0xCCFFFFFF)
val FrostedSurface = Color(0xE6FFFFFF)
val AccentSoftBg = Color(0xFFFCE8F3)

// Gradient chính Hồng Quark -> Tím Hồng -> Tím Spin
val AccentGradient = Brush.linearGradient(
    listOf(AccentRed, QuantumMagenta, AccentPurple)
)

// Gradient Quang Phổ Lượng Tử (thuần Hồng - Tím, không Cyan)
val QuantumSpectrumBrush = Brush.linearGradient(
    listOf(AccentRed, QuantumMagenta, AccentPurple, QuantumViolet)
)

// Gradient viền khúc xạ lăng kính cho các khối Neumorphic (Hồng - Tím)
val QuantumPrismBorderBrush = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.95f),
        AccentRed.copy(alpha = 0.32f),
        QuantumMagenta.copy(alpha = 0.28f),
        AccentPurple.copy(alpha = 0.32f),
        Color.White.copy(alpha = 0.72f)
    )
)
