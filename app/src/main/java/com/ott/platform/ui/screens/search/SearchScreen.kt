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

@Composable
fun SearchScreen(
    onNavigateToContent: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
            .padding(spacing.screenPadding)
    ) {
        OttTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search movies, shows, and more...",
            imeAction = ImeAction.Search,
            onImeAction = {
                // Trigger search
            }
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
        
        // Placeholder state
        EmptySearchState()
    }
}
