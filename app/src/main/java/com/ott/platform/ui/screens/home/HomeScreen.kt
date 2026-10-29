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
import androidx.compose.ui.unit.sp
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

    // Helper to filter content based on selected language
    val filterByLang: (List<Content>) -> List<Content> = remember(uiState.selectedLanguage) {
        { list ->
            when (uiState.selectedLanguage) {
                "Hindi" -> list.filter { it.language.equals("Hindi", ignoreCase = true) }
                "English" -> list.filter { it.language.equals("English", ignoreCase = true) }
                else -> list
            }
        }
    }

    val displayNewReleases = remember(uiState.newReleases, uiState.selectedLanguage) {
        filterByLang(uiState.newReleases)
    }
    val displayTrending = remember(uiState.trending, uiState.selectedLanguage) {
        filterByLang(uiState.trending)
    }
    val displayHindi = remember(uiState.hindiContent, uiState.selectedLanguage) {
        if (uiState.selectedLanguage == "English") emptyList() else uiState.hindiContent
    }
    val displayEnglish = remember(uiState.englishContent, uiState.selectedLanguage) {
        if (uiState.selectedLanguage == "Hindi") emptyList() else uiState.englishContent
    }
    val displayAction = remember(uiState.actionContent, uiState.selectedLanguage) {
        filterByLang(uiState.actionContent)
    }
    val displayComedy = remember(uiState.comedyContent, uiState.selectedLanguage) {
        filterByLang(uiState.comedyContent)
    }
    val displayRomance = remember(uiState.romanceContent, uiState.selectedLanguage) {
        filterByLang(uiState.romanceContent)
    }
    val displayPopular = remember(uiState.popular, uiState.selectedLanguage) {
        filterByLang(uiState.popular)
    }

    val hasAnyContent = displayNewReleases.isNotEmpty() ||
        displayTrending.isNotEmpty() ||
        displayHindi.isNotEmpty() ||
        displayEnglish.isNotEmpty() ||
        displayAction.isNotEmpty() ||
        displayComedy.isNotEmpty() ||
        displayRomance.isNotEmpty() ||
        displayPopular.isNotEmpty()

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

        // Language Filter Chips
        item {
            Spacer(modifier = Modifier.height(spacing.md))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val languages = listOf("ALL" to "All", "Hindi" to "Hindi", "English" to "English")
                languages.forEach { (code, label) ->
                    val isSelected = uiState.selectedLanguage.equals(code, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setLanguage(code) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else OttTextPrimary
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OttGold,
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = OttTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) OttGold else Color(0xFF334155),
                            borderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }

        if (!hasAnyContent) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp, horizontal = spacing.screenPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No content available for the selected filter.",
                        color = OttTextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // 1. New Releases Section (Latest 10 items, newest first)
        if (displayNewReleases.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "New Releases",
                    items = displayNewReleases,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // 2. Trending Section
        if (displayTrending.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Trending Now",
                    items = displayTrending,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // 3. Hindi Cinema & Originals
        if (displayHindi.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Hindi Cinema & Originals",
                    items = displayHindi,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // 4. English Cinema & Series
        if (displayEnglish.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "English Cinema & Series",
                    items = displayEnglish,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // 5. Action Packed
        if (displayAction.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Action Packed",
                    items = displayAction,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // 6. Comedy Specials
        if (displayComedy.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Comedy Specials",
                    items = displayComedy,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // 7. Romance & Drama
        if (displayRomance.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Romance & Drama",
                    items = displayRomance,
                    onItemClick = onNavigateToContent
                )
            }
        }

        // 8. Popular Section
        if (displayPopular.isNotEmpty() && uiState.selectedLanguage == "ALL") {
            item {
                Spacer(modifier = Modifier.height(spacing.sectionGap))
                ContentRow(
                    title = "Popular on Chumbak OTT",
                    items = displayPopular,
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
            .height(480.dp)
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
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                )

                // Bottom Fade gradient into dark background for crisp text legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.88f),
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
                        .padding(horizontal = 20.dp, vertical = 20.dp),
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
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Title - Large, bold, and high-contrast
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                                blurRadius = 24f
                            )
                        ),
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                blurRadius = 12f
                            )
                        ),
                        color = Color(0xFFE2E8F0),
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
