package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Streamwise Cinematic Palette
val AmberGoldPrimary = Color(0xFFF59E0B)
val AmberGoldContainer = Color(0xFF332002)
val OnAmberGoldContainer = Color(0xFFFDE68A)

val ElectricCyanSecondary = Color(0xFF38BDF8)
val CyanContainer = Color(0xFF082F49)
val OnCyanContainer = Color(0xFFBAE6FD)

val CineRedTertiary = Color(0xFFF43F5E)
val RedContainer = Color(0xFF4C0519)
val OnRedContainer = Color(0xFFFECDD3)

// Dark Mode Elevation Tokens
val DarkCanvas = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF121622)
val DarkSurfaceVariant = Color(0xFF1B2030)
val DarkSurfaceElevated = Color(0xFF242B40)
val DarkBorderOutline = Color(0x40FFFFFF) // 25% white border for high visibility
val DarkOutlineVariant = Color(0x2EFFFFFF) // 18% white outline (up from 8% to pass WCAG contrast)
val DarkInputBackground = Color(0x14FFFFFF) // 8% white surface fill for distinct input fields

// Text Contrast Tokens (Softened off-white to prevent OLED halation, WCAG AA compliant)
val TextHighEmphasis = Color(0xE6FFFFFF)  // 90% white
val TextMediumEmphasis = Color(0xB3FFFFFF) // 70% white
val TextLowEmphasis = Color(0x9EFFFFFF)    // 62% white (improved from 45% for readable placeholders)

// Status & Badge indicators
val StatusSuccess = Color(0xFF10B981)
val StatusWarning = Color(0xFFF59E0B)
val StatusError = Color(0xFFEF4444)
val FreeBadgeContainer = Color(0xFF064E3B) // Dark Emerald Container for Free w/ ads
val OnFreeBadgeContainer = Color(0xFF6EE7B7) // High contrast emerald label
