package com.ott.platform.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ott.platform.BuildConfig
import com.ott.platform.R
import com.ott.platform.ui.theme.*

private const val FEATURED_BACKDROP_URL =
    "https://s3.in-west2.purestore.io/chumbak/posters/upl_1792312345569_tmrp6on_Hilarious_Police_Station_Chaos.png"

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
    ) {
        // ── 1. Theatrical Backdrop Poster ────────────────────────────────────
        AsyncImage(
            model = FEATURED_BACKDROP_URL,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ── 2. Cinematic Gradient Scrims ─────────────────────────────────────
        // Top vignette
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Dark gradient floor for contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.88f),
                            Color(0xFF0A0A0C),
                            Color(0xFF0A0A0C)
                        )
                    )
                )
        )

        // Ambient radial amber glow
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            ChumbakAmber.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // ── 3. Scrollable Glass Content ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top back button bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = Color(0x661A1A1E),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                ) {
                    IconButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Sign In",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Header Branding ──
            Surface(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(OttGold, OttGoldDark, Color(0x44F59E0B))
                            )
                        ),
                        RoundedCornerShape(16.dp)
                    ),
                color = Color(0xFF141416),
                shadowElevation = 10.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chumbak_logo),
                    contentDescription = BuildConfig.APP_NAME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Create Your Account",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Join ${BuildConfig.APP_NAME} for unlimited streaming",
                style = MaterialTheme.typography.bodyMedium,
                color = OttTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Glassmorphic Form Card ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xE6141418), // Rich 90% opacity frosted slate
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x55FBBF24),
                            Color(0x22FFFFFF),
                            Color(0x0AFFFFFF)
                        )
                    )
                ),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Full Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Full Name", color = OttTextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = OttGold,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OttGold,
                            focusedBorderColor = OttGold,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF19191D),
                            unfocusedContainerColor = Color(0xFF19191D)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mobile Number Field
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { input ->
                            mobile = input.filter { it.isDigit() }.take(10)
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("10-digit mobile number", color = OttTextTertiary) },
                        leadingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = OttGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+91",
                                    fontWeight = FontWeight.Bold,
                                    color = OttGold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(Color(0x44FFFFFF))
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OttGold,
                            focusedBorderColor = OttGold,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF19191D),
                            unfocusedContainerColor = Color(0xFF19191D)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Address Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Email address", color = OttTextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = OttGold,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OttGold,
                            focusedBorderColor = OttGold,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF19191D),
                            unfocusedContainerColor = Color(0xFF19191D)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Password (min. 6 characters)", color = OttTextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = OttGold,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = OttTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OttGold,
                            focusedBorderColor = OttGold,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF19191D),
                            unfocusedContainerColor = Color(0xFF19191D)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            validationError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Confirm password", color = OttTextTertiary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = OttGold,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (confirmPassword.isNotEmpty() && password.isNotEmpty()) {
                                    Icon(
                                        imageVector = if (confirmPassword == password) Icons.Default.CheckCircle else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (confirmPassword == password) OttTeal else OttError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                        tint = OttTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = OttGold,
                            focusedBorderColor = OttGold,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0xFF19191D),
                            unfocusedContainerColor = Color(0xFF19191D)
                        )
                    )

                    // Error Message Display
                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0x22CF6679),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x55CF6679)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = validationError.orEmpty(),
                                color = OttError,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0x22CF6679),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0x55CF6679)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (authState as AuthState.Error).message,
                                color = OttError,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sign Up CTA Button
                    val isFormValid = name.isNotBlank() &&
                            mobile.length == 10 &&
                            email.contains("@") &&
                            password.length >= 6 &&
                            password == confirmPassword

                    LuxuryGradientButton(
                        text = "Create Account & Start Watching",
                        onClick = {
                            focusManager.clearFocus()
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
                        enabled = isFormValid && !isLoading
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "By creating an account, you agree to Chumbak OTT Terms of Service and Privacy Policy.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        color = OttTextTertiary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation to Login Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OttTextSecondary
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = OttGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
