package com.ott.platform.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttGold

/**
 * Frictionless Registration:
 * Real users register/sign-in directly via Phone Number + OTP or Google Sign-In on the LoginScreen.
 * No multi-field registration form is required.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        onNavigateToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = OttGold)
    }
}
