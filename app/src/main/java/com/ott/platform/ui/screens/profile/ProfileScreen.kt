package com.ott.platform.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.components.OttTextButton
import com.ott.platform.ui.theme.*

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
            .padding(spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Profile & Account",
            style = MaterialTheme.typography.headlineMedium,
            color = OttRed,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(spacing.xxl))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return
        }

        // Avatar with Initials
        val initial = uiState.name.firstOrNull()?.uppercase() ?: uiState.email.firstOrNull()?.uppercase() ?: "U"
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(spacing.md))

        Text(
            text = uiState.name,
            style = MaterialTheme.typography.titleLarge,
            color = OttTextPrimary,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = uiState.email,
            style = MaterialTheme.typography.bodyMedium,
            color = OttTextSecondary
        )

        if (uiState.isAdmin) {
            Spacer(modifier = Modifier.height(spacing.xs))
            Surface(
                color = OttRed.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Administrator",
                    color = OttRed,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))

        // Subscription Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = OttSurfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Subscription Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OttTextPrimary
                    )

                    val badgeColor = if (uiState.hasActiveSubscription) Color(0xFF10B981) else Color(0xFFEF4444)
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = uiState.subscriptionStatus.uppercase(),
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.sm))

                Text(
                    text = uiState.planName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OttTextPrimary,
                    fontWeight = FontWeight.Medium
                )

                if (uiState.expiryDate != null) {
                    Spacer(modifier = Modifier.height(spacing.xs))
                    Text(
                        text = "Valid until: ${uiState.expiryDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = OttTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(spacing.md))

                OttButton(
                    text = if (uiState.hasActiveSubscription) "Change Subscription" else "Upgrade to Premium",
                    onClick = onNavigateToSubscription,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OttTextButton(
            text = "Sign Out",
            onClick = onSignOut
        )

        Spacer(modifier = Modifier.height(spacing.bottomNavHeight))
    }
}
