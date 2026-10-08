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
import com.ott.platform.ui.theme.OttRed

@Composable
fun MyListScreen(
    onNavigateToContent: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    
    // Placeholder empty list state
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
        
        // When empty:
        EmptyListState()
        
        // When loading:
        // SkeletonGrid()
    }
}
