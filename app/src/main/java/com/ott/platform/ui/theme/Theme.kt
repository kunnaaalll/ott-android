package com.ott.platform.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * OTT Platform Theme
 *
 * Always dark — this is a cinematic streaming app.
 * The system dark mode toggle is intentionally ignored.
 */

private val OttDarkColorScheme = darkColorScheme(
    // Primary
    primary = OttRed,
    onPrimary = Color.White,
    primaryContainer = OttRedDark,
    onPrimaryContainer = Color.White,

    // Secondary
    secondary = OttGold,
    onSecondary = Color.Black,
    secondaryContainer = OttGoldDark,
    onSecondaryContainer = Color.White,

    // Tertiary
    tertiary = OttTeal,
    onTertiary = Color.Black,
    tertiaryContainer = OttTealDark,
    onTertiaryContainer = Color.White,

    // Background
    background = OttBackground,
    onBackground = OttTextPrimary,

    // Surface
    surface = OttSurface,
    onSurface = OttTextPrimary,
    surfaceVariant = OttSurfaceVariant,
    onSurfaceVariant = OttTextSecondary,

    // Error
    error = OttError,
    onError = Color.Black,
    errorContainer = OttErrorDark,
    onErrorContainer = Color.White,

    // Outline
    outline = OttBorder,
    outlineVariant = OttDivider,

    // Inverse (for snackbars, etc.)
    inverseSurface = Color.White,
    inverseOnSurface = OttBlack,
    inversePrimary = OttRedDark,

    // Scrim
    scrim = Color.Black,
)

@Composable
fun OttTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = OttDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = OttBackground.toArgb()
            window.navigationBarColor = OttBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OttTypography,
            shapes = OttShapes,
            content = content,
        )
    }
}
