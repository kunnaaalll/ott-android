package com.ott.platform.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ott.platform.BuildConfig
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.components.OttTextButton
import com.ott.platform.ui.components.OttTextField
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttRed
import com.ott.platform.ui.theme.OttSurfaceVariant
import com.ott.platform.ui.theme.OttTextPrimary
import com.ott.platform.ui.theme.OttTextSecondary
import com.ott.platform.ui.theme.OttTextTertiary

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
import kotlinx.coroutines.delay

enum class LoginMode {
    OTP,
    PASSWORD
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val spacing = LocalContext.current.let { LocalSpacing.current }
    var loginMode by remember { mutableStateOf(LoginMode.OTP) }

    // Password mode states
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // OTP mode states
    var mobile by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading
    val context = LocalContext.current

    // Countdown timer for OTP resend
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

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

        Spacer(modifier = Modifier.height(spacing.lg))

        // Mode Switcher Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(OttSurfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (loginMode == LoginMode.OTP) OttRed else OttSurfaceVariant)
                    .clickable {
                        loginMode = LoginMode.OTP
                        infoMessage = null
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Login with OTP",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (loginMode == LoginMode.OTP) FontWeight.Bold else FontWeight.Normal,
                    color = if (loginMode == LoginMode.OTP) OttTextPrimary else OttTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (loginMode == LoginMode.PASSWORD) OttRed else OttSurfaceVariant)
                    .clickable {
                        loginMode = LoginMode.PASSWORD
                        infoMessage = null
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Password Login",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (loginMode == LoginMode.PASSWORD) FontWeight.Bold else FontWeight.Normal,
                    color = if (loginMode == LoginMode.PASSWORD) OttTextPrimary else OttTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        if (loginMode == LoginMode.OTP) {
            // OTP Flow
            OttTextField(
                value = mobile,
                onValueChange = {
                    mobile = it.filter { ch -> ch.isDigit() }.take(10)
                },
                placeholder = "10-digit Mobile Number",
                keyboardType = KeyboardType.Phone,
            )

            if (otpSent) {
                Spacer(modifier = Modifier.height(spacing.md))

                OttTextField(
                    value = otpCode,
                    onValueChange = {
                        otpCode = it.filter { ch -> ch.isDigit() }.take(6)
                    },
                    placeholder = "Enter 6-digit OTP",
                    keyboardType = KeyboardType.NumberPassword,
                )
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            if (!infoMessage.isNullOrBlank()) {
                Text(
                    text = infoMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = spacing.sm)
                )
            }

            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = spacing.md)
                )
            }

            if (!otpSent) {
                OttButton(
                    text = "Send OTP",
                    onClick = {
                        viewModel.sendOtp(mobile) { msg ->
                            otpSent = true
                            countdown = 30
                            infoMessage = "OTP sent to +91 $mobile. (Test bypass OTP: 123456)"
                        }
                    },
                    isLoading = isLoading,
                    enabled = mobile.length == 10 && !isLoading
                )
            } else {
                OttButton(
                    text = "Verify & Sign In",
                    onClick = {
                        viewModel.verifyOtp(mobile, otpCode)
                    },
                    isLoading = isLoading,
                    enabled = otpCode.length == 6 && !isLoading
                )

                Spacer(modifier = Modifier.height(spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (countdown > 0) {
                        Text(
                            text = "Resend OTP in ${countdown}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = OttTextSecondary
                        )
                    } else {
                        OttTextButton(
                            text = "Resend OTP",
                            onClick = {
                                viewModel.sendOtp(mobile) { msg ->
                                    countdown = 30
                                    infoMessage = "New OTP sent to +91 $mobile."
                                }
                            }
                        )
                    }
                }
            }

        } else {
            // Password Flow
            OttTextField(
                value = identifier,
                onValueChange = { identifier = it },
                placeholder = "Email or Mobile Number",
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

            Spacer(modifier = Modifier.height(spacing.lg))

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
                    viewModel.signIn(identifier, password)
                },
                isLoading = isLoading,
                enabled = identifier.isNotBlank() && password.isNotBlank() && !isLoading
            )
        }

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
