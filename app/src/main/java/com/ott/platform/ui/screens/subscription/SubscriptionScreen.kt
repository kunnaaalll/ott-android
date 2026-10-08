package com.ott.platform.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.components.OttTextButton
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttBorderFocused
import com.ott.platform.ui.theme.OttGold
import com.ott.platform.ui.theme.OttSurfaceVariant
import com.ott.platform.ui.theme.OttTextPrimary
import com.ott.platform.ui.theme.OttTextSecondary

@Composable
fun SubscriptionScreen(
    onSubscribeSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
            .padding(spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(spacing.xl))
        
        Text(
            text = "WATCH EVERYTHING",
            style = MaterialTheme.typography.displaySmall,
            color = OttGold,
            fontWeight = FontWeight.Bold,
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
        
        Text(
            text = "Unlimited access to the complete library.",
            style = MaterialTheme.typography.titleMedium,
            color = OttTextSecondary,
        )
        
        Spacer(modifier = Modifier.height(spacing.xl))
        
        // Features list
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            val features = listOf(
                "✓ Full content library",
                "✓ New releases",
                "✓ HD / 4K where available",
                "✓ Continue watching",
                "✓ Premium streaming"
            )
            features.forEach { feature ->
                Text(
                    text = feature,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OttTextPrimary,
                    modifier = Modifier.padding(vertical = spacing.xs)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(spacing.xxl))
        
        // Plan Card (Placeholder for active plan selection)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, OttBorderFocused, RoundedCornerShape(12.dp))
                .background(OttSurfaceVariant)
                .padding(spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "₹199 / Month",
                    style = MaterialTheme.typography.headlineLarge,
                    color = OttTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cancel anytime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OttTextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        OttButton(
            text = "START SUBSCRIPTION",
            onClick = onSubscribeSuccess
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
        
        OttTextButton(
            text = "Not now",
            onClick = onNavigateBack
        )
        
        Spacer(modifier = Modifier.height(spacing.md))
    }
}
