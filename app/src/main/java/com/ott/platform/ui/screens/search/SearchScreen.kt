package com.ott.platform.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.ott.platform.ui.components.EmptySearchState
import com.ott.platform.ui.components.OttTextField
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.items
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SearchScreen(
    onNavigateToContent: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
            .padding(spacing.screenPadding)
    ) {
        OttTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            placeholder = "Search movies, shows, and more...",
            imeAction = ImeAction.Search,
            onImeAction = { /* No-op, search is reactive */ }
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
        
        if (uiState.isSearching) {
            // Placeholder for loading
            androidx.compose.material3.CircularProgressIndicator(
                color = com.ott.platform.ui.theme.OttRed,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally).padding(top = spacing.lg)
            )
        } else if (uiState.hasSearched && uiState.results.isEmpty()) {
            EmptySearchState()
        } else {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(110.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = uiState.results,
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
