package com.ott.platform.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

@Composable
fun HomeScreen(
    onNavigateToContent: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    
    // Placeholder loading state
    val isLoading = false
    
    if (isLoading) {
        SkeletonHomeScreen(modifier = Modifier.fillMaxSize().background(OttBackground))
        return
    }

    LazyColumn(
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
                // Placeholder for hero trailer/poster
            }
        }
        
        // Continue Watching Row
        item {
            Spacer(modifier = Modifier.height(spacing.sectionGap))
            ContentRow(
                title = "Continue Watching",
                items = listOf("1", "2", "3"),
                isWide = true,
                onItemClick = onNavigateToContent
            )
        }
        
        // New Releases Row
        item {
            Spacer(modifier = Modifier.height(spacing.sectionGap))
            ContentRow(
                title = "New Releases",
                items = listOf("4", "5", "6", "7", "8"),
                isWide = false,
                onItemClick = onNavigateToContent
            )
        }
        
        // Trending Row
        item {
            Spacer(modifier = Modifier.height(spacing.sectionGap))
            ContentRow(
                title = "Trending Now",
                items = listOf("9", "10", "11", "12", "13"),
                isWide = false,
                onItemClick = onNavigateToContent
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(spacing.bottomNavHeight + spacing.lg))
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    items: List<String>,
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
            items(items) { id ->
                if (isWide) {
                    ContentCardWide(
                        title = "Content $id",
                        backdropUrl = null,
                        onClick = { onItemClick(id) }
                    )
                } else {
                    ContentCard(
                        title = "Content $id",
                        posterUrl = null,
                        onClick = { onItemClick(id) }
                    )
                }
            }
        }
    }
}
