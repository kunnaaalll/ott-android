package com.ott.platform.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * OTT Platform — Cinematic Dark Color Palette
 *
 * Inspired by premium streaming services (Netflix, Disney+, Apple TV+).
 * Deep blacks with vibrant accent colors for a theatrical feel.
 */

// ── Primary Brand: Chumbak OTT ──────────────────────────────────────
val ChumbakAmber = Color(0xFFF59E0B)       // Signature glowing amber
val ChumbakAmberDark = Color(0xFFD97706)   // Pressed / deep amber
val ChumbakAmberLight = Color(0xFFFBBF24)  // Golden highlight / badges
val ChumbakPink = Color(0xFFEC4899)        // Accent spark magenta/pink
val ChumbakViolet = Color(0xFF8B5CF6)      // Accent spark violet
val ChumbakBlack = Color(0xFF000000)       // Pure black background

// Aliases for unified system-wide UI integration:
val OttRed = ChumbakAmber
val OttRedDark = ChumbakAmberDark
val OttRedLight = ChumbakAmberLight

// ── Accent / Secondary ─────────────────────────────────────────────
val OttGold = Color(0xFFFBBF24)           // Premium / subscription accents
val OttGoldDark = Color(0xFFD97706)
val OttTeal = Color(0xFF1DB9AA)           // Success states, active indicators
val OttTealDark = Color(0xFF17978B)

// ── Surface Hierarchy (dark → light) ────────────────────────────────
val OttBlack = Color(0xFF000000)          // True black — AMOLED friendly
val OttBackground = Color(0xFF0A0A0A)     // App background
val OttSurface = Color(0xFF141414)        // Cards, bottom sheets
val OttSurfaceVariant = Color(0xFF1C1C1E) // Elevated cards, inputs
val OttSurfaceElevated = Color(0xFF252528)// Dialogs, popups
val OttSurfaceBright = Color(0xFF2C2C30)  // Hover states, selection

// ── Text ────────────────────────────────────────────────────────────
val OttTextPrimary = Color(0xFFFFFFFF)    // Headlines, primary text
val OttTextSecondary = Color(0xFFB3B3B3)  // Body text, descriptions
val OttTextTertiary = Color(0xFF808080)   // Captions, timestamps
val OttTextDisabled = Color(0xFF4D4D4D)   // Disabled labels

// ── Borders & Dividers ──────────────────────────────────────────────
val OttDivider = Color(0xFF2A2A2A)
val OttBorder = Color(0xFF333333)
val OttBorderFocused = Color(0xFF555555)

// ── Semantic ────────────────────────────────────────────────────────
val OttError = Color(0xFFCF6679)
val OttErrorDark = Color(0xFFB00020)
val OttSuccess = Color(0xFF4CAF50)
val OttWarning = Color(0xFFFF9800)
val OttInfo = Color(0xFF2196F3)

// ── Gradient Colors ─────────────────────────────────────────────────
val OttGradientStart = Color(0xFF0A0A0A)
val OttGradientMid = Color(0x990A0A0A)
val OttGradientEnd = Color(0x000A0A0A)
val OttHeroGradientTop = Color(0x00000000)
val OttHeroGradientBottom = Color(0xFF0A0A0A)

// ── Shimmer ─────────────────────────────────────────────────────────
val OttShimmerBase = Color(0xFF1C1C1E)
val OttShimmerHighlight = Color(0xFF2A2A2D)
