package com.ott.platform.ui.components.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.screens.content.PlaybackState
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttGold
import com.ott.platform.ui.theme.OttTextPrimary
import com.ott.platform.ui.theme.OttTextSecondary
import kotlinx.coroutines.delay

/**
 * Streaming resolution options for smooth playback.
 */
enum class VideoQuality(
    val label: String,
    val shortLabel: String,
    val maxHeight: Int,
    val maxBitrate: Int
) {
    AUTO("Auto (recommended)", "Auto", 4320, Int.MAX_VALUE),
    FHD_1080("1080p", "1080p", 1080, 8_000_000),
    HD_720("720p", "720p", 720, 3_500_000),
    SD_480("480p", "480p", 480, 1_500_000),
    SAVER_360("360p", "360p", 360, 800_000);
}

@OptIn(UnstableApi::class)
@Composable
fun OttVideoPlayer(
    playbackState: PlaybackState,
    playbackUrl: String?,
    cookies: Map<String, String> = emptyMap(),
    onStateChanged: (PlaybackState) -> Unit,
    onRetry: () -> Unit,
    onGoToSubscription: () -> Unit,
    onGoToLogin: () -> Unit = {},
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onFullscreenToggle: () -> Unit = {}
) {
    val context = LocalContext.current

    // Quality track selector
    val trackSelector = remember {
        DefaultTrackSelector(context)
    }

    // High performance LoadControl tuned specifically for mobile networks:
    // Starts almost immediately (1.5s buffer) and recovers rapidly from mobile signal drops (2.5s)
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000, // minBufferMs (15s minimum buffer)
                60_000, // maxBufferMs (up to 60s buffer ahead)
                1_500,  // bufferForPlaybackMs (instant playback start)
                2_500   // bufferForPlaybackAfterRebufferMs (rapid recovery after rebuffering)
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build().apply {
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }

    // Quality controls state (YouTube-style bottom sheet)
    var selectedQuality by remember { mutableStateOf(VideoQuality.AUTO) }
    var showQualitySheet by remember { mutableStateOf(false) }

    // Quick seek gesture indicators
    var rewindIndicatorVisible by remember { mutableStateOf(false) }
    var forwardIndicatorVisible by remember { mutableStateOf(false) }

    // Hardware back dismisses quality sheet first
    if (showQualitySheet) {
        BackHandler {
            showQualitySheet = false
        }
    }

    fun applyQuality(quality: VideoQuality) {
        selectedQuality = quality
        val builder = trackSelector.parameters.buildUpon()
        if (quality == VideoQuality.AUTO) {
            builder.clearVideoSizeConstraints()
            builder.setMaxVideoBitrate(Int.MAX_VALUE)
        } else {
            builder.setMaxVideoSize(1920, quality.maxHeight)
            builder.setMaxVideoBitrate(quality.maxBitrate)
        }
        trackSelector.parameters = builder.build()
    }

    fun seekForward() {
        val dur = exoPlayer.duration
        val newPos = if (dur > 0) minOf(dur, exoPlayer.currentPosition + 10_000L) else exoPlayer.currentPosition + 10_000L
        exoPlayer.seekTo(newPos)
        currentPosition = newPos
        showControls = true
    }

    fun seekBackward() {
        val newPos = maxOf(0L, exoPlayer.currentPosition - 10_000L)
        exoPlayer.seekTo(newPos)
        currentPosition = newPos
        showControls = true
    }

    // Auto-hide indicators
    LaunchedEffect(rewindIndicatorVisible) {
        if (rewindIndicatorVisible) {
            delay(650)
            rewindIndicatorVisible = false
        }
    }
    LaunchedEffect(forwardIndicatorVisible) {
        if (forwardIndicatorVisible) {
            delay(650)
            forwardIndicatorVisible = false
        }
    }

    // Auto-hide controls after inactivity (paused if quality sheet is open)
    LaunchedEffect(showControls, isPlaying, showQualitySheet) {
        if (showControls && isPlaying && !showQualitySheet) {
            delay(3500)
            showControls = false
        }
    }

    // Continuously update position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(500)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> onStateChanged(PlaybackState.Buffering)
                    Player.STATE_READY -> {
                        duration = exoPlayer.duration
                        onStateChanged(if (exoPlayer.playWhenReady) PlaybackState.Playing else PlaybackState.Paused)
                    }
                    Player.STATE_ENDED -> onStateChanged(PlaybackState.Completed)
                    Player.STATE_IDLE -> { /* idle */ }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                onStateChanged(PlaybackState.Error("Playback failed", "PLAYER_ERROR"))
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Validate playback URL and attach CloudFront cookies
    LaunchedEffect(playbackUrl, cookies) {
        if (playbackUrl != null) {
            val uri = android.net.Uri.parse(playbackUrl)
            val scheme = uri.scheme
            val host = uri.host?.lowercase() ?: ""

            if (scheme != "https") {
                android.util.Log.e("OttVideoPlayer", "Refusing to attach cookies over insecure connection.")
                onStateChanged(PlaybackState.Error("Playback configuration error.", "INSECURE_CONNECTION"))
                return@LaunchedEffect
            }

            val configuredHost = com.ott.platform.BuildConfig.CLOUDFRONT_HOSTNAME
            if (configuredHost.isEmpty()) {
                android.util.Log.e("OttVideoPlayer", "CloudFront hostname not configured in BuildConfig.")
                onStateChanged(PlaybackState.Error("Playback configuration error.", "MISSING_CONFIG"))
                return@LaunchedEffect
            }

            val isCloudFrontHost = (host == configuredHost) ||
                    host == "s3.in-west2.purestore.io" ||
                    host.endsWith(".purestore.io") ||
                    host == "test-streams.mux.dev"
            if (!isCloudFrontHost) {
                android.util.Log.e("OttVideoPlayer", "Refusing to attach cookies to non-CloudFront host.")
                onStateChanged(PlaybackState.Error("Playback configuration error.", "INVALID_HOST"))
                return@LaunchedEffect
            }

            val isPrivateHost = (host == configuredHost) || host.endsWith("purestore.io")
            val cookieString = if (isPrivateHost) cookies.entries.joinToString("; ") { "${it.key}=${it.value}" } else ""
            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setUserAgent("ChumbakOTT-Android/1.0.7")
                if (cookieString.isNotEmpty()) {
                    setDefaultRequestProperties(mapOf("Cookie" to cookieString))
                }
            }
            val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(playbackUrl))

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
        } else {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width * 0.35f) {
                            seekBackward()
                            rewindIndicatorVisible = true
                        } else if (offset.x > width * 0.65f) {
                            seekForward()
                            forwardIndicatorVisible = true
                        } else {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            showControls = true
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    }
                )
            }
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // States
        val isError = playbackState is PlaybackState.Error
        val isAuthorizing = playbackState is PlaybackState.Authorizing
        val isBuffering = playbackState is PlaybackState.Buffering || playbackState is PlaybackState.Preparing

        // Double-tap visual feedback overlays
        if (rewindIndicatorVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Filled.Replay10, contentDescription = null, tint = OttGold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("-10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (forwardIndicatorVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("+10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Filled.Forward10, contentDescription = null, tint = OttGold)
                }
            }
        }

        // Overlay & Controls (Always available during buffering!)
        AnimatedVisibility(
            visible = showControls || !isPlaying || isError || isBuffering || isAuthorizing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                if (isError) {
                    val errorState = playbackState as PlaybackState.Error
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorState.message,
                            color = OttTextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (errorState.reason == "SUBSCRIPTION_REQUIRED") {
                            OttButton(text = "Subscribe Now", onClick = onGoToSubscription)
                        } else if (errorState.reason == "UNAUTHENTICATED") {
                            OttButton(text = "Sign In to Watch", onClick = onGoToLogin)
                        } else {
                            OttButton(text = "Retry", onClick = onRetry)
                        }
                    }
                } else {
                    // Center Controls: Rewind 10s, Play/Pause with Buffering indicator, Forward 10s
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // -10s Rewind Button
                        IconButton(
                            onClick = { seekBackward() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        // Play/Pause Button with integrated buffering spinner
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBuffering || isAuthorizing) {
                                CircularProgressIndicator(
                                    color = OttGold,
                                    strokeWidth = 3.5.dp,
                                    modifier = Modifier.size(68.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    showControls = true
                                },
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(if (isBuffering || isAuthorizing) Color.Black.copy(alpha = 0.7f) else OttGold)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = if (isBuffering || isAuthorizing) Color.White else Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // +10s Fast-Forward Button
                        IconButton(
                            onClick = { seekForward() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    // Bottom Bar (Scrubber, Timers, Settings Gear, Fullscreen)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                            onValueChange = { percent ->
                                val newPosition = (percent * duration).toLong()
                                currentPosition = newPosition
                                exoPlayer.seekTo(newPosition)
                                showControls = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = OttGold,
                                activeTrackColor = OttGold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )

                        Text(
                            text = formatTime(duration),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Clean Settings Gear (YouTube style)
                        IconButton(
                            onClick = { showQualitySheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Quality Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Fullscreen Toggle Button
                        IconButton(
                            onClick = onFullscreenToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // YouTube-style Quality Bottom Sheet
        AnimatedVisibility(
            visible = showQualitySheet,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showQualitySheet = false
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(if (isFullscreen) 0.55f else 1f)
                        .wrapContentHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Consume clicks inside sheet */ },
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    color = Color(0xFF212121),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 12.dp)
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 4.dp, bottom = 10.dp)
                                .width(36.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                        )

                        // Title Header (like YouTube)
                        Text(
                            text = "Quality for current video",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.12f),
                            thickness = 0.5.dp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Clean YouTube-style Resolution items
                        VideoQuality.values().forEach { quality ->
                            val isSelected = quality == selectedQuality
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        applyQuality(quality)
                                        showQualitySheet = false
                                    }
                                    .padding(horizontal = 24.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quality.label,
                                    color = if (isSelected) OttGold else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = OttGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
