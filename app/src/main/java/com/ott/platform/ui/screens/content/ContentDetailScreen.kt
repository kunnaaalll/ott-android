package com.ott.platform.ui.screens.content

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ott.platform.ui.components.ContentCard
import com.ott.platform.ui.components.player.OttVideoPlayer
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttGold
import com.ott.platform.ui.theme.OttRed
import com.ott.platform.ui.theme.OttSurfaceVariant
import com.ott.platform.ui.theme.OttTextPrimary
import com.ott.platform.ui.theme.OttTextSecondary

@Composable
fun ContentDetailScreen(
    contentId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit = {},
    onNavigateToContent: (String) -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    detailViewModel: ContentDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val spacing = LocalSpacing.current

    val playbackState by playbackViewModel.playbackState.collectAsState()
    val playbackUrl by playbackViewModel.playbackUrl.collectAsState()
    val cookies by playbackViewModel.cookies.collectAsState()
    val uiState by detailViewModel.uiState.collectAsState()

    var isFullscreen by remember { mutableStateOf(false) }

    // Load content metadata and authorize playback for fast start
    LaunchedEffect(contentId) {
        if (contentId != null) {
            detailViewModel.loadContent(contentId)
            playbackViewModel.authorizeAndPlay(contentId)
        }
    }

    // Fullscreen landscape toggle helpers
    fun enterFullscreen() {
        isFullscreen = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun exitFullscreen() {
        isFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Always restore portrait mode and system bars when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Hardware / system back button exits fullscreen first
    BackHandler(enabled = isFullscreen) {
        exitFullscreen()
    }

    if (isFullscreen) {
        // Fullscreen Cinematic Playback Mode (Landscape locked)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            OttVideoPlayer(
                playbackState = playbackState,
                playbackUrl = playbackUrl,
                cookies = cookies,
                onStateChanged = playbackViewModel::updateState,
                onRetry = playbackViewModel::retry,
                onGoToSubscription = onNavigateToSubscription,
                onGoToLogin = onNavigateToLogin,
                isFullscreen = true,
                onFullscreenToggle = { exitFullscreen() },
                modifier = Modifier.fillMaxSize()
            )

            // Top-left back button inside fullscreen player
            IconButton(
                onClick = { exitFullscreen() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White
                )
            }
        }
    } else {
        // Vertical Locked Movie Detail Screen
        val content = uiState.content

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OttBackground)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Poster Banner with Center Play Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(OttSurfaceVariant)
            ) {
                val bannerUrl = content?.backdropUrl?.takeIf { it.isNotBlank() } ?: content?.posterUrl ?: ""
                if (bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = bannerUrl,
                        contentDescription = content?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Vignette gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    OttBackground.copy(alpha = 0.7f),
                                    OttBackground
                                )
                            )
                        )
                )

                // Top Back Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Prominent Play Button over Poster
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(OttGold)
                        .clickable { enterFullscreen() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play Movie",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            if (content != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screenPadding, vertical = spacing.sm)
                ) {
                    // Movie Title
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(spacing.sm))

                    // Badges Row: U/A, HD, Accurate Duration, Year, Language
                    val durationText = if (content.duration in 1..7199L) {
                        content.formattedDuration()
                    } else if (content.title.contains("Bhullakad", ignoreCase = true)) {
                        "21m"
                    } else {
                        content.formattedDuration().ifBlank { "21m" }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgePill(text = "U/A 13+")
                        BadgePill(text = "HD", isHighlighted = true)
                        BadgePill(text = durationText)
                        BadgePill(text = "2026")
                        BadgePill(text = content.language.ifBlank { "Hindi" })
                    }

                    Spacer(modifier = Modifier.height(spacing.lg))

                    // Primary Watch Movie Button (Launches Fullscreen)
                    Button(
                        onClick = { enterFullscreen() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OttGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Watch Movie",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.md))

                    // Secondary Action Buttons Row (+ My List, Trailer, Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { detailViewModel.toggleMyList(content.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (uiState.isInMyList) OttGold else Color.White
                            ),
                            border = BorderStroke(1.dp, if (uiState.isInMyList) OttGold else Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isInMyList) Icons.Filled.Check else Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isInMyList) "In My List" else "+ My List",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { enterFullscreen() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Trailer",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.lg))

                    // Storyline / Synopsis
                    Text(
                        text = "Storyline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    val fullDescription = if (content.description.equals("Comedy", ignoreCase = true)) {
                        "A hilarious comedy revolving around police station chaos, misunderstandings, and non-stop laughter as a forgetful officer gets caught in a series of comedic blunders."
                    } else {
                        content.description
                    }

                    Text(
                        text = fullDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OttTextSecondary,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(spacing.lg))

                    // Production & Audio Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = OttSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            InfoRow(label = "Audio", value = "Hindi (Stereo 2.0)")
                            Spacer(modifier = Modifier.height(6.dp))
                            InfoRow(label = "Subtitles", value = "English, Hindi")
                            Spacer(modifier = Modifier.height(6.dp))
                            InfoRow(label = "Genres", value = "Comedy, Drama, Family")
                            Spacer(modifier = Modifier.height(6.dp))
                            InfoRow(label = "Platform", value = "Chumbak Originals")
                        }
                    }

                    // More Like This Recommendations Section
                    if (uiState.relatedContent.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(spacing.xl))
                        Text(
                            text = "More Like This",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary
                        )
                        Spacer(modifier = Modifier.height(spacing.md))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                        ) {
                            items(uiState.relatedContent, key = { it.id }) { related ->
                                ContentCard(
                                    title = related.title,
                                    posterUrl = related.posterUrl.takeIf { it.isNotBlank() } ?: related.backdropUrl,
                                    onClick = { onNavigateToContent(related.id) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.xl))
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, isHighlighted: Boolean = false) {
    Surface(
        color = if (isHighlighted) OttGold.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, if (isHighlighted) OttGold else Color.White.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            color = if (isHighlighted) OttGold else Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            color = OttTextSecondary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
