package com.ott.platform.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for consistent layout throughout the app.
 * Access via LocalSpacing.current inside any composable.
 */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val default: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val huge: Dp = 48.dp,
    val massive: Dp = 64.dp,

    // Specific use-case tokens
    val screenPadding: Dp = 16.dp,
    val cardPadding: Dp = 12.dp,
    val sectionGap: Dp = 24.dp,
    val contentRowHeight: Dp = 180.dp,
    val posterWidth: Dp = 120.dp,
    val posterHeight: Dp = 170.dp,
    val heroHeight: Dp = 420.dp,
    val bottomNavHeight: Dp = 64.dp,
    val iconSize: Dp = 24.dp,
    val iconSizeLarge: Dp = 32.dp,
    val buttonHeight: Dp = 52.dp,
    val inputHeight: Dp = 56.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
