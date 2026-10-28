package com.ott.platform.ui.components.player

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.screens.content.PlaybackState
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttTextPrimary
import kotlinx.coroutines.delay

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
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    
    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    // Update position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            delay(1000)
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
                    Player.STATE_IDLE -> { /* Do nothing or handle idle */ }
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

    // SECURITY: Validate the playback URL host before attaching CloudFront cookies.
    // Cookies must only be sent to the CloudFront domain — not to arbitrary hosts.
    // This guards against server-side misconfigurations or future code changes
    // that could route requests to a non-CloudFront endpoint.
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

            // Only accept exactly configured hostnames, purestore media storage, or reviewer demo host
            val isCloudFrontHost = (host == configuredHost) ||
                    host == "s3.in-west2.purestore.io" ||
                    host.endsWith(".purestore.io") ||
                    host == "test-streams.mux.dev"
            if (!isCloudFrontHost) {
                // Do not attach cookies to non-CloudFront hosts.
                // Log without the URL to avoid leaking it.
                android.util.Log.e("OttVideoPlayer", "Refusing to attach cookies to non-CloudFront host.")
                onStateChanged(PlaybackState.Error("Playback configuration error.", "INVALID_HOST"))
                return@LaunchedEffect
            }

            val isPrivateHost = (host == configuredHost) || host.endsWith("purestore.io")
            val cookieString = if (isPrivateHost) cookies.entries.joinToString("; ") { "${it.key}=${it.value}" } else ""
            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setUserAgent("ChumbakOTT-Android/1.0.2")
                if (cookieString.isNotEmpty()) {
                    setDefaultRequestProperties(mapOf("Cookie" to cookieString))
                }
            }
            val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(playbackUrl))
                
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
        } else {
            // Clear the player when authorization is revoked or content changes.
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
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

        // Overlay & Controls
        val isError = playbackState is PlaybackState.Error
        val isAuthorizing = playbackState is PlaybackState.Authorizing
        val isBuffering = playbackState is PlaybackState.Buffering || playbackState is PlaybackState.Preparing

        AnimatedVisibility(
            visible = showControls || !isPlaying || isError || isAuthorizing || isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                if (isAuthorizing || isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (isError) {
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
                    // Playback Controls
                    // Center Play/Pause
                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            showControls = true
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom Bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            onValueChange = { percent ->
                                val newPosition = (percent * duration).toLong()
                                currentPosition = newPosition
                                exoPlayer.seekTo(newPosition)
                                showControls = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onFullscreenToggle) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White
                            )
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
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
