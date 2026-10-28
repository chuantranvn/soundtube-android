package com.soundtube.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ===== Bảng màu Quantum Frosted Neumorphic (Hồng Quark - Tím Spin Lượng Tử, Không dùng Cyan) =====
// Nền ngọc trai pha sắc Hồng Phấn -> Tím Oải Hương dịu mắt theo chiều dọc (Top-to-Bottom)
val NeuBase = Color(0xFFF2ECF9)
val NeuLightShadow = Color(0xFFFFFFFF)
val NeuDarkShadow = Color(0xFFC9C0DC)

// Các dải màu cho Gradient nền từ trên xuống dưới (Top -> Bottom)
val BgGradientTop = Color(0xFFFCF3F8)       // Đỉnh màn hình: Trắng hồng phấn cực dịu
val BgGradientUpperMid = Color(0xFFF5EEFC)  // Giữa trên: Trắng ngọc trai ánh oải hương
val BgGradientLowerMid = Color(0xFFEDE4F8)  // Giữa dưới: Sương mù tím nhạt neumorphic
val BgGradientBottom = Color(0xFFE3D8F3)    // Đáy màn hình: Tím khói chiều sâu lượng tử

val DarkBackground = NeuBase
val DarkSurface = Color(0xFFF8F4FC)
val DarkSurfaceVariant = Color(0xFFE8E0F4)

// Bộ màu Hạt Quark & Trường Spin Lượng Tử (Hồng San Hô -> Tím Hồng -> Tím Spin -> Tím Oải Hương)
val AccentRed = Color(0xFFFF2E63)       // Hồng năng lượng Quark
val AccentRedLight = Color(0xFFFF6B93)
val AccentPurple = Color(0xFF7C4DFF)    // Tím trường lượng tử Spin
val QuantumMagenta = Color(0xFFD838B8)  // Tím hồng giao thoa sóng
val QuantumViolet = Color(0xFFA855F7)   // Tím thạch anh lượng tử (thay thế hoàn toàn Cyan)
val QuantumCyan = QuantumViolet         // Giữ alias tương thích ngược nhưng mang sắc Tím Lượng Tử

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
