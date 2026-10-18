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

import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.ui.screens.auth.AuthViewModel
import com.ott.platform.ui.screens.auth.AuthState

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onRegisterSuccess()
        }
    }

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
        
        Spacer(modifier = Modifier.height(spacing.xl))
        
        OttTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Full Name",
            keyboardType = KeyboardType.Text,
        )
        
        Spacer(modifier = Modifier.height(spacing.sm))
        
        OttTextField(
            value = mobile,
            onValueChange = { mobile = it.filter { ch -> ch.isDigit() }.take(10) },
            placeholder = "10-digit Mobile Number",
            keyboardType = KeyboardType.Phone,
        )
        
        Spacer(modifier = Modifier.height(spacing.sm))

        OttTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email address",
            keyboardType = KeyboardType.Email,
        )
        
        Spacer(modifier = Modifier.height(spacing.sm))
        
        OttTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password (min. 6 characters)",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
        
        Spacer(modifier = Modifier.height(spacing.sm))
        
        OttTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = "Confirm Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
        )
        
        Spacer(modifier = Modifier.height(spacing.lg))
        
        if (validationError != null) {
            Text(
                text = validationError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = spacing.md)
            )
        } else if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = spacing.md)
            )
        }

        OttButton(
            text = "Sign Up",
            onClick = {
                validationError = null
                val cleanMobile = mobile.filter { it.isDigit() }
                when {
                    name.isBlank() -> validationError = "Please enter your full name."
                    cleanMobile.length != 10 -> validationError = "Please enter a valid 10-digit mobile number."
                    !email.contains("@") -> validationError = "Please enter a valid email address."
                    password.length < 6 -> validationError = "Password must be at least 6 characters long."
                    password != confirmPassword -> validationError = "Passwords do not match."
                    else -> viewModel.register(name, email, cleanMobile, password)
                }
            },
            isLoading = isLoading,
            enabled = name.isNotBlank() && mobile.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
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
