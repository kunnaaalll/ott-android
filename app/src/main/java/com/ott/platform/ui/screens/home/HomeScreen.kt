package com.ott.platform.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ott.platform.data.model.Content
import com.ott.platform.ui.components.ContentCard
import com.ott.platform.ui.components.SkeletonHomeScreen
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttGold
import com.ott.platform.ui.theme.OttRed
import com.ott.platform.ui.theme.OttTextPrimary
import com.ott.platform.ui.theme.OttTextSecondary

@Composable
fun HomeScreen(
    onNavigateToContent: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        SkeletonHomeScreen(modifier = Modifier.fillMaxSize().background(OttBackground))
        return
    }

    // Determine banner items for the top swipeable carousel
    val bannerItems = remember(uiState) {
        val list = mutableListOf<Content>()
        if (uiState.featured.isNotEmpty()) list.addAll(uiState.featured)
        if (uiState.newReleases.isNotEmpty()) {
            uiState.newReleases.forEach { if (!list.contains(it)) list.add(it) }
        }
        if (list.isEmpty() && uiState.trending.isNotEmpty()) list.addAll(uiState.trending)
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
    ) {
        // Top Swipeable Hero Banner Carousel (Posters only — NO auto-playing video)
        if (bannerItems.isNotEmpty()) {
            item {
                HeroBannerCarousel(
                    items = bannerItems,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // New Releases Section
        if (uiState.newReleases.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "New Releases",
                    items = uiState.newReleases,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // Trending Section
        if (uiState.trending.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Trending Now",
                    items = uiState.trending,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // Popular Section
        if (uiState.popular.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Popular on Chumbak OTT",
                    items = uiState.popular,
                    onItemClick = onNavigateToContent
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(spacing.bottomNavHeight + spacing.lg))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun HeroBannerCarousel(
    items: List<Content>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val content = items[page]
            val imageUrl = content.backdropUrl.ifBlank { content.posterUrl }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onItemClick(content.id) }
            ) {
                // Background Banner Poster
                AsyncImage(
                    model = imageUrl,
                    contentDescription = content.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Vignette gradient for clean status bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                            )
                        )
                )

                // Bottom Fade gradient into dark background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.75f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    OttBackground.copy(alpha = 0.6f),
                                    OttBackground
                                )
                            )
                        )
                )

                // Information overlay and Watch Now action
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Badge
                    Surface(
                        color = OttRed,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = if (content.featured) "FEATURED" else "NEW RELEASE",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Title
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Duration and Language
                    val metaParts = mutableListOf<String>()
                    val dur = content.formattedDuration()
                    if (dur.isNotBlank()) metaParts.add(dur)
                    if (content.language.isNotBlank()) metaParts.add(content.language)
                    if (content.description.isNotBlank()) metaParts.add(content.description)
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = OttTextSecondary,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onItemClick(content.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OttGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Watch Now",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onItemClick(content.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Details",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Pager indicator dots (when multiple items exist)
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(items.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) OttGold else Color.White.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    items: List<Content>,
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
                ContentCard(
                    title = content.title,
                    posterUrl = content.posterUrl.takeIf { it.isNotBlank() } ?: content.backdropUrl,
                    onClick = { onItemClick(content.id) }
                )
            }
        }
    }
}
