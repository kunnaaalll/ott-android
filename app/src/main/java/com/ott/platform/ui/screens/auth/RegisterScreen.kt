package com.ott.platform.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ott.platform.BuildConfig
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.components.OttTextButton
import com.ott.platform.ui.components.OttTextField
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttRed
import com.ott.platform.ui.theme.OttTextSecondary

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val spacing = LocalSpacing.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
            .padding(spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.displaySmall,
            color = OttRed,
            fontWeight = FontWeight.Bold,
        )
        
        Spacer(modifier = Modifier.height(spacing.xxl))
        
        OttTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email address",
            keyboardType = KeyboardType.Email,
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
        
        OttTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
        
        OttTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "Confirm Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
        
        Spacer(modifier = Modifier.height(spacing.xl))
        
        OttButton(
            text = "Sign Up",
            onClick = {
                isLoading = true
                onRegisterSuccess()
            },
            isLoading = isLoading,
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = OttTextSecondary,
            )
            OttTextButton(
                text = "Sign in",
                onClick = onNavigateToLogin,
            )
        }
    }
}
