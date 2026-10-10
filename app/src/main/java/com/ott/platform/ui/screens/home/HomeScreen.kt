package com.ott.platform.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ott.platform.ui.components.ContentCard
import com.ott.platform.ui.components.ContentCardWide
import com.ott.platform.ui.components.SkeletonHomeScreen
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttTextPrimary

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import com.ott.platform.ui.components.player.HeroTrailerPlayer

@Composable
fun HomeScreen(
    onNavigateToContent: (String) -> Unit,
    heroTrailerController: HeroTrailerController = hiltViewModel(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    
    val playbackUrl by heroTrailerController.playbackUrl.collectAsState()
    val cookies by heroTrailerController.cookies.collectAsState()
    val isMuted by heroTrailerController.isMuted.collectAsState()
    
    val uiState by viewModel.uiState.collectAsState()
    
    if (uiState.isLoading) {
        SkeletonHomeScreen(modifier = Modifier.fillMaxSize().background(OttBackground))
        return
    }

    val listState = rememberLazyListState()
    val isHeroVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0
        }
    }

    val heroContent = uiState.featured.firstOrNull()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground),
    ) {
        // Hero Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.heroHeight)
                    .background(com.ott.platform.ui.theme.OttSurfaceVariant)
            ) {
                if (heroContent != null) {
                    val heroContentId = heroContent.id
                    
                    DisposableEffect(heroContentId) {
                        onDispose {
                            heroTrailerController.onHeroInactive(heroContentId)
                        }
                    }
                    
                    LaunchedEffect(isHeroVisible, heroContentId) {
                        if (isHeroVisible) {
                            heroTrailerController.onHeroActive(heroContentId)
                        } else {
                            heroTrailerController.onHeroInactive(heroContentId)
                        }
                    }

                    HeroTrailerPlayer(
                        playbackUrl = playbackUrl,
                        cookies = cookies,
                        isMuted = isMuted,
                        onToggleMute = heroTrailerController::toggleMute,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        // Continue Watching Row - if implemented later
        if (uiState.continueWatching.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Continue Watching",
                    items = uiState.continueWatching,
                    isWide = true,
                    onItemClick = onNavigateToContent
                )
            }
        }
        
        // New Releases Row
        if (uiState.newReleases.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "New Releases",
                    items = uiState.newReleases,
                    isWide = false,
                    onItemClick = onNavigateToContent
                )
            }
        }
        
        // Trending Row
        if (uiState.trending.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Trending Now",
                    items = uiState.trending,
                    isWide = false,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // Popular Row
        if (uiState.popular.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Popular on OTT",
                    items = uiState.popular,
                    isWide = false,
                    onItemClick = onNavigateToContent
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(spacing.bottomNavHeight + spacing.lg))
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    items: List<com.ott.platform.data.model.Content>,
    isWide: Boolean,
    onItemClick: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OttTextPrimary,
            modifier = Modifier.padding(horizontal = spacing.screenPadding)
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            items(items, key = { it.id }) { content ->
                if (isWide) {
                    ContentCardWide(
                        title = content.title,
                        backdropUrl = content.backdropUrl.takeIf { it.isNotBlank() } ?: content.posterUrl,
                        onClick = { onItemClick(content.id) }
                    )
                } else {
                    ContentCard(
                        title = content.title,
                        posterUrl = content.posterUrl.takeIf { it.isNotBlank() } ?: content.backdropUrl,
                        onClick = { onItemClick(content.id) }
                    )
                }
            }
        }
    }
}
