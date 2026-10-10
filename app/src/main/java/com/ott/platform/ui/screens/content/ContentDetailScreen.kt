package com.ott.platform.ui.screens.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.components.OttOutlinedButton
import com.ott.platform.ui.components.SkeletonHero
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttTextPrimary
import com.ott.platform.ui.theme.OttTextSecondary

import androidx.compose.ui.platform.LocalConfiguration
import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.ui.screens.content.PlaybackViewModel
import com.ott.platform.ui.screens.content.PlaybackState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import com.ott.platform.ui.components.player.OttVideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    contentId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit, // Unused as we have inline player, but kept for signature
    onNavigateToContent: (String) -> Unit,
    playbackViewModel: PlaybackViewModel = hiltViewModel(),
    detailViewModel: ContentDetailViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val playbackUrl by playbackViewModel.playbackUrl.collectAsState()
    val cookies by playbackViewModel.cookies.collectAsState()
    var isFullscreen by remember { mutableStateOf(false) }

    val uiState by detailViewModel.uiState.collectAsState()

    LaunchedEffect(contentId) {
        if (contentId != null) {
            playbackViewModel.authorizeAndPlay(contentId)
            detailViewModel.loadContent(contentId)
        }
    }
    
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    val isLoading = false // Placeholder

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = OttBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isFullscreen) {
                OttVideoPlayer(
                    playbackState = playbackState,
                    playbackUrl = playbackUrl,
                    cookies = cookies,
                    onStateChanged = playbackViewModel::updateState,
                    onRetry = playbackViewModel::retry,
                    onGoToSubscription = { /* Navigate to sub */ },
                    isFullscreen = true,
                    onFullscreenToggle = { isFullscreen = false },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spacing.heroHeight / 1.5f)
                        .background(com.ott.platform.ui.theme.OttSurfaceVariant)
                ) {
                    OttVideoPlayer(
                        playbackState = playbackState,
                        playbackUrl = playbackUrl,
                        cookies = cookies,
                        onStateChanged = playbackViewModel::updateState,
                        onRetry = playbackViewModel::retry,
                        onGoToSubscription = { /* Navigate to sub */ },
                        isFullscreen = false,
                        onFullscreenToggle = { isFullscreen = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            if (!isFullscreen) {
                Column(
                    modifier = Modifier.padding(spacing.screenPadding)
                ) {
                    val content = uiState.content
                    if (content != null) {
                        Text(
                            text = content.title,
                            style = MaterialTheme.typography.displaySmall,
                            color = OttTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(spacing.xs))
                        
                        // Extract year from timestamp if present, otherwise just show other metadata
                        val year = content.releaseDate?.toDate()?.let {
                            java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(it)
                        } ?: ""
                        
                        val metaString = listOf(year, content.formattedDuration(), content.language)
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")
                        
                        Text(
                            text = metaString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = OttTextSecondary
                        )
                        
                        Spacer(modifier = Modifier.height(spacing.lg))
                        
                        OttOutlinedButton(
                            text = if (uiState.isInMyList) "✓ In My List" else "+ My List",
                            onClick = { detailViewModel.toggleMyList(content.id) }
                        )
                        
                        Spacer(modifier = Modifier.height(spacing.lg))
                        
                        Text(
                            text = content.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = OttTextPrimary
                        )
                    }

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
                                com.ott.platform.ui.components.ContentCard(
                                    title = related.title,
                                    posterUrl = related.posterUrl.takeIf { it.isNotBlank() } ?: related.backdropUrl,
                                    onClick = { onNavigateToContent(related.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
