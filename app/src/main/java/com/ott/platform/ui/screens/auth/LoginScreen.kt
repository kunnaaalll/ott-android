package com.ott.platform.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
private const val FEATURED_BACKDROP_URL =
    "https://s3.in-west2.purestore.io/chumbak/posters/upl_1792312345569_tmrp6on_Hilarious_Police_Station_Chaos.png"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // OTP mode states
    var mobile by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading

    var showGoogleEmailDialog by remember { mutableStateOf(false) }
    var directGoogleEmail by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_CANCELED) {
            return@rememberLauncherForActivityResult
        }

        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            var apiException: ApiException? = null
            val account = try {
                task.getResult(ApiException::class.java)
            } catch (e: ApiException) {
                apiException = e
                null
            } catch (e: Exception) {
                null
            }

            val email = account?.email
                ?: data.getStringExtra("authAccount")
                ?: data.getStringExtra("accountName")
                ?: data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
                ?: data.extras?.let { bundle ->
                    bundle.keySet().firstNotNullOfOrNull { key ->
                        val v = bundle.get(key)?.toString()
                        if (v != null && v.contains("@") && v.contains(".") && !v.contains(" ") && v.length > 5) v else null
                    }
                }

            if (!email.isNullOrBlank()) {
                val displayName = account?.displayName
                    ?: email.substringBefore("@").replace(".", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                val photoUrl = account?.photoUrl?.toString() ?: ""
                val googleId = account?.id ?: "acc_${email.hashCode()}"

                viewModel.signInWithGoogleAccount(
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    googleId = googleId
                )
            } else {
                val statusCode = apiException?.statusCode
                val message = when (statusCode) {
                    10 -> "Google Sign-In Error (10): Release SHA-1 fingerprint is not registered in Firebase Console."
                    12500 -> "Google Sign-In Error (12500): Sign in failed. Check Google Play Services."
                    7 -> "Network error during Google Sign-In. Please check your connection."
                    else -> "Google account sign-in failed${if (statusCode != null) " (code: $statusCode)" else ""}."
                }
                android.util.Log.e("LoginScreen", "Google Sign-In failed: $message, ex=$apiException")
                infoMessage = message
            }
        }
    }

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

                            Spacer(modifier = Modifier.height(14.dp))

                            // ── OR Divider ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0x22FFFFFF)
                                )
                                Text(
                                    text = "OR",
                                    color = OttTextTertiary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0x22FFFFFF)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ── Google Sign In Button ──
                            Button(
                                onClick = {
                                    try {
                                        val clientId = context.getString(com.ott.platform.R.string.default_web_client_id)
                                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(clientId)
                                            .requestEmail()
                                            .build()
                                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                        googleSignInClient.signOut().addOnCompleteListener {
                                            try {
                                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                            } catch (e: Exception) {
                                                android.util.Log.w("LoginScreen", "Google Sign-In launch error: ${e.message}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.w("LoginScreen", "Google Sign-In client error: ${e.message}")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = com.ott.platform.R.drawable.ic_google_logo),
                                        contentDescription = "Google",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Continue with Google",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF1F1F24)
                                    )
                                }
                            }
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

            // ── Seamless Access Footer ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Instant Access • No forms or passwords required",
                    style = MaterialTheme.typography.bodySmall,
                    color = OttTextSecondary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Sign in or join automatically with your mobile number or Google",
                    style = MaterialTheme.typography.labelSmall,
                    color = OttTextTertiary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Direct Google Sign-In Fallback Dialog
        if (showGoogleEmailDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showGoogleEmailDialog = false },
                containerColor = Color(0xFF1E1E24),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = com.ott.platform.R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Choose or enter your Google email address to sign in immediately:",
                            style = MaterialTheme.typography.bodySmall,
                            color = OttTextSecondary
                        )
                        OutlinedTextField(
                            value = directGoogleEmail,
                            onValueChange = { directGoogleEmail = it },
                            placeholder = { Text("your.email@gmail.com", color = Color(0xFF666666)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = OttGold,
                                unfocusedBorderColor = Color(0x44FFFFFF)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val cleanEmail = directGoogleEmail.trim().lowercase()
                            if (cleanEmail.contains("@") && cleanEmail.contains(".")) {
                                showGoogleEmailDialog = false
                                viewModel.signInWithGoogleAccount(
                                    email = cleanEmail,
                                    displayName = cleanEmail.substringBefore("@")
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OttGold)
                    ) {
                        Text("Sign In", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoogleEmailDialog = false }) {
                        Text("Cancel", color = OttTextSecondary)
                    }
                }
            )
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
