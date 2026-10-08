package com.ott.platform.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.ott.platform.BuildConfig
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttRed
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
) {
    // In a real app, this might check auth state and subscription status.
    // For MVP shell, we just wait a moment and move on.
    LaunchedEffect(Unit) {
        delay(1500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = BuildConfig.APP_NAME.uppercase(),
            style = MaterialTheme.typography.displayMedium,
            color = OttRed,
            fontWeight = FontWeight.Bold,
        )
    }
}
