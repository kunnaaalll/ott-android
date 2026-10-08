package com.ott.platform.ui.screens.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDetailScreen(
    contentId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    
    val isLoading = false // Placeholder
    
    if (isLoading) {
        SkeletonHero(modifier = Modifier.fillMaxSize().background(OttBackground))
        return
    }

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
            // Hero Backdrop placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spacing.heroHeight / 1.5f)
                    .background(com.ott.platform.ui.theme.OttSurfaceVariant)
            )
            
            Column(
                modifier = Modifier.padding(spacing.screenPadding)
            ) {
                Text(
                    text = "Content Title $contentId",
                    style = MaterialTheme.typography.displaySmall,
                    color = OttTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(spacing.xs))
                
                Text(
                    text = "2026 • 2h 15m • Action, Sci-Fi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OttTextSecondary
                )
                
                Spacer(modifier = Modifier.height(spacing.lg))
                
                OttButton(
                    text = "Play",
                    onClick = { contentId?.let { onNavigateToPlayer(it) } }
                )
                
                Spacer(modifier = Modifier.height(spacing.sm))
                
                OttOutlinedButton(
                    text = "+ My List",
                    onClick = { /* TODO */ }
                )
                
                Spacer(modifier = Modifier.height(spacing.lg))
                
                Text(
                    text = "This is a detailed description of the content. It gives the user context about what they are about to watch. It should be compelling and concise.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OttTextPrimary
                )
            }
        }
    }
}
