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

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val spacing = LocalContext.current.let { LocalSpacing.current }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading
    
    val context = LocalContext.current
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    viewModel.signInWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                // Handle exception
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
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
            text = BuildConfig.APP_NAME.uppercase(),
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
        
        Spacer(modifier = Modifier.height(spacing.xl))
        
        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = spacing.md)
            )
        }

        OttButton(
            text = "Sign In",
            onClick = {
                viewModel.signIn(email, password)
            },
            isLoading = isLoading,
            enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
        )
        
        Spacer(modifier = Modifier.height(spacing.lg))
        
        Text(
            text = "OR",
            style = MaterialTheme.typography.labelMedium,
            color = OttTextSecondary,
        )
        
        Spacer(modifier = Modifier.height(spacing.lg))
        
        OttButton(
            text = "Sign in with Google",
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(com.ott.platform.R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "New to ${BuildConfig.APP_NAME}?",
                style = MaterialTheme.typography.bodyMedium,
                color = OttTextSecondary,
            )
            OttTextButton(
                text = "Sign up now",
                onClick = onNavigateToRegister,
            )
        }
    }
}
