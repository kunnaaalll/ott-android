package com.ott.platform.ui.screens.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ott.platform.ui.components.EmptyListState
import com.ott.platform.ui.components.SkeletonGrid
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.ott.platform.ui.theme.OttRed

@Composable
fun MyListScreen(
    onNavigateToContent: (String) -> Unit,
    viewModel: MyListViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
            .padding(spacing.screenPadding)
    ) {
        Text(
            text = "My List",
            style = MaterialTheme.typography.headlineMedium,
            color = OttRed,
            fontWeight = FontWeight.Bold,
        )
        
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(spacing.md))
        
        if (uiState.isLoading) {
            SkeletonGrid()
        } else if (uiState.items.isEmpty()) {
            EmptyListState()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.items,
                    key = { it.id }
                ) { content ->
                    com.ott.platform.ui.components.ContentCard(
                        title = content.title,
                        posterUrl = content.posterUrl.takeIf { it.isNotBlank() } ?: content.backdropUrl,
                        onClick = { onNavigateToContent(content.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
