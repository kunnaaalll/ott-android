package com.ott.platform.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay

enum class LoginMode {
    OTP,
    PASSWORD
}

private const val FEATURED_BACKDROP_URL =
    "https://s3.in-west2.purestore.io/chumbak/posters/upl_1792312345569_tmrp6on_Hilarious_Police_Station_Chaos.png"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var loginMode by remember { mutableStateOf(LoginMode.OTP) }

    // Password mode states
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // OTP mode states
    var mobile by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading

    // Countdown timer for OTP resend
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }



    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
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
            modifier = Modifier
                .fillMaxSize()
        )

        // ── 2. Cinematic Gradient Scrims ─────────────────────────────────────
        // Top subtle dark vignette
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

        // Bottom to center heavy dark gradient for maximum contrast
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

        // Ambient radial amber glow behind the center card
        Box(
            modifier = Modifier
                .size(360.dp)
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

        // ── 3. Scrollable Interactive Glass Content ──────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Branded Header ──
            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                listOf(OttGold, OttGoldDark, Color(0x44F59E0B))
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    ),
                color = Color(0xFF141416),
                shadowElevation = 12.dp
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chumbak_logo),
                    contentDescription = BuildConfig.APP_NAME,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = BuildConfig.APP_NAME.uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Premium tag pill
            Surface(
                color = Color(0x33F59E0B),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, Color(0x55FBBF24))
            ) {
                Text(
                    text = "CINEMATIC ENTERTAINMENT • STREAM ANYWHERE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = OttGold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // ── Glassmorphic Card Container ──
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
                    // ── Pill Segmented Tab Switcher ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1F1F24))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // OTP Tab
                        val isOtp = loginMode == LoginMode.OTP
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isOtp) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable {
                                    loginMode = LoginMode.OTP
                                    infoMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = if (isOtp) Color.Black else OttTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mobile OTP",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isOtp) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isOtp) Color.Black else OttTextSecondary
                                )
                            }
                        }

                        // Password Tab
                        val isPass = loginMode == LoginMode.PASSWORD
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isPass) {
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFD97706), Color(0xFFF59E0B))
                                        )
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable {
                                    loginMode = LoginMode.PASSWORD
                                    infoMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isPass) Color.Black else OttTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isPass) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isPass) Color.Black else OttTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Active Flow Body ──
                    if (loginMode == LoginMode.OTP) {
                        // ── OTP Flow ──
                        if (!otpSent) {
                            // Phone input field
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { input ->
                                    mobile = input.filter { it.isDigit() }.take(10)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("10-digit mobile number", color = OttTextTertiary)
                                },
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
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (mobile.length == 10 && !isLoading) {
                                            viewModel.sendOtp(mobile) {
                                                otpSent = true
                                                countdown = 30
                                                infoMessage = "OTP sent to +91 $mobile"
                                            }
                                        }
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

                            Spacer(modifier = Modifier.height(16.dp))

                            // Send OTP Button
                            LuxuryGradientButton(
                                text = "Get Verification OTP",
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.sendOtp(mobile) {
                                        otpSent = true
                                        countdown = 30
                                        infoMessage = "OTP sent to +91 $mobile"
                                    }
                                },
                                isLoading = isLoading,
                                enabled = mobile.length == 10 && !isLoading
                            )
                        } else {
                            // OTP Verification Screen
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0x221DB9AA),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0x441DB9AA))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = OttTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+91 $mobile",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Change",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = OttGold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                otpSent = false
                                                otpCode = ""
                                                infoMessage = null
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // OTP Code Input Field
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { input ->
                                    otpCode = input.filter { it.isDigit() }.take(6)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text("Enter 6-digit OTP code", color = OttTextTertiary)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = OttGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (otpCode.length == 6 && !isLoading) {
                                            viewModel.verifyOtp(mobile, otpCode)
                                        }
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

                            Spacer(modifier = Modifier.height(10.dp))

                            // One-tap bypass / tester helper pill
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        otpCode = "123456"
                                    },
                                color = Color(0x22FBBF24),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0x44FBBF24))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "⚡ Reviewer Quick Code: ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OttTextSecondary
                                    )
                                    Text(
                                        text = "123456",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = OttGold
                                    )
                                    Text(
                                        text = " (Tap to fill)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OttGold.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Verify & Sign In Button
                            LuxuryGradientButton(
                                text = "Verify & Start Streaming",
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.verifyOtp(mobile, otpCode)
                                },
                                isLoading = isLoading,
                                enabled = otpCode.length == 6 && !isLoading
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Resend timer row
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
                                    Text(
                                        text = "Resend OTP",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = OttGold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                viewModel.sendOtp(mobile) {
                                                    countdown = 30
                                                    infoMessage = "New OTP sent to +91 $mobile"
                                                }
                                            }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }

                    } else {
                        // ── Password Flow ──
                        // Email / Mobile Field
                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Email address or phone", color = OttTextTertiary)
                            },
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Enter password", color = OttTextTertiary)
                            },
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
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (identifier.isNotBlank() && password.isNotBlank() && !isLoading) {
                                        viewModel.signIn(identifier, password)
                                    }
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

                        Spacer(modifier = Modifier.height(18.dp))

                        // Sign In Button
                        LuxuryGradientButton(
                            text = "Sign In to Account",
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.signIn(identifier, password)
                            },
                            isLoading = isLoading,
                            enabled = identifier.isNotBlank() && password.isNotBlank() && !isLoading
                        )
                    }

                    // ── Error / Info Message ──
                    if (!infoMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = infoMessage.orEmpty(),
                            color = OttTeal,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(10.dp))
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


                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Register Navigation Footer ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "New to ${BuildConfig.APP_NAME}?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OttTextSecondary
                )
                TextButton(
                    onClick = onNavigateToRegister,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Sign Up Now",
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

/**
 * Reusable glowing gradient action button for auth screens.
 */
@Composable
fun LuxuryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val gradientBrush = if (enabled && !isLoading) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFD97706),
                Color(0xFFF59E0B),
                Color(0xFFFBBF24)
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF3A3A40),
                Color(0xFF2C2C30)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(gradientBrush)
            .clickable(enabled = enabled && !isLoading) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.Black,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = if (enabled) Color.Black else OttTextTertiary
            )
        }
    }
}
