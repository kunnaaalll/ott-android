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

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.ui.screens.auth.AuthViewModel
import com.ott.platform.ui.screens.auth.AuthState

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ott.platform.R
import com.ott.platform.ui.theme.ChumbakAmberLight

@Composable
fun SplashScreen(
    onSplashFinished: (isAuthenticated: Boolean) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        // Only trigger navigation if auth state is resolved (not Idle or Loading if we treat Idle as unresolved)
        // Actually AuthViewModel starts by observing Firebase user.
        // Idle -> Authenticated or Unauthenticated.
        delay(800) // Small delay for splash visibility
        when (authState) {
            is AuthState.Authenticated -> onSplashFinished(true)
            is AuthState.Unauthenticated -> onSplashFinished(false)
            is AuthState.Error -> onSplashFinished(false)
            else -> { /* Still resolving */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.chumbak_logo),
                contentDescription = BuildConfig.APP_NAME,
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = BuildConfig.APP_NAME.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = ChumbakAmberLight,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}
