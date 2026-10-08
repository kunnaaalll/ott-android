package com.ott.platform.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.components.OttTextButton
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttRed
import com.ott.platform.ui.theme.OttTextPrimary
import com.ott.platform.ui.theme.OttTextSecondary

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val spacing = LocalSpacing.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
            .padding(spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = OttRed,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(spacing.xxl))
        
        // Placeholder Avatar
        Box(
            modifier = Modifier
                .size(spacing.massive * 2)
                .background(com.ott.platform.ui.theme.OttSurfaceVariant, shape = androidx.compose.foundation.shape.CircleShape)
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
        
        Text(
            text = "John Doe",
            style = MaterialTheme.typography.titleLarge,
            color = OttTextPrimary
        )
        
        Text(
            text = "john.doe@example.com",
            style = MaterialTheme.typography.bodyMedium,
            color = OttTextSecondary
        )
        
        Spacer(modifier = Modifier.height(spacing.xl))
        
        OttButton(
            text = "Manage Subscription",
            onClick = onNavigateToSubscription
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        OttTextButton(
            text = "Sign Out",
            onClick = onSignOut
        )
        
        Spacer(modifier = Modifier.height(spacing.bottomNavHeight))
    }
}
