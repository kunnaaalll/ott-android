package com.ott.platform.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.data.model.Plan
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
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onSubscribeSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val spacing = LocalSpacing.current
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    val plans by viewModel.plans.collectAsState()
    val isPaymentProcessing by viewModel.isPaymentProcessing.collectAsState()

    var selectedPlan by remember { mutableStateOf<Plan?>(null) }

    // If subscription somehow became active directly while on this screen
    if (subscriptionState is SubscriptionState.Active) {
        onSubscribeSuccess()
        return
    }

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

        if (subscriptionState is SubscriptionState.Pending || subscriptionState is SubscriptionState.Confirming) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = OttGold)
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = if (subscriptionState is SubscriptionState.Confirming) {
                        "Payment Complete\nWe're confirming your subscription...\nPlease wait."
                    } else {
                        "Payment Processing\nWaiting for provider..."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = OttTextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // Plan Selection
            if (plans.isEmpty()) {
                CircularProgressIndicator(color = OttGold)
            } else {
                if (selectedPlan == null) selectedPlan = plans.first()
                plans.forEach { plan ->
                    val isSelected = selectedPlan == plan
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.sm)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                2.dp,
                                if (isSelected) OttBorderFocused else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .background(OttSurfaceVariant)
                            .clickable { selectedPlan = plan }
                            .padding(spacing.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = plan.formattedPrice(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = OttTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = plan.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OttTextSecondary
                            )
                        }
                    }
                }
            }
            
            if (subscriptionState is SubscriptionState.ConfirmingTimeout) {
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = "We're still waiting for confirmation.\nYou can retry checking your subscription later.",
                    color = OttGold,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (subscriptionState is SubscriptionState.Error) {
                Spacer(modifier = Modifier.height(spacing.md))
                Text(
                    text = (subscriptionState as SubscriptionState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            OttButton(
                text = "START SUBSCRIPTION",
                onClick = { selectedPlan?.let { viewModel.initiatePayment(it) } },
                enabled = !isPaymentProcessing && selectedPlan != null && subscriptionState !is SubscriptionState.Pending
            )
            
            Spacer(modifier = Modifier.height(spacing.md))
            
            OttTextButton(
                text = "Not now",
                onClick = onNavigateBack
            )
        }
        
        Spacer(modifier = Modifier.height(spacing.md))
    }
}
