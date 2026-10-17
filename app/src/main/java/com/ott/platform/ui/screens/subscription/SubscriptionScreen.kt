package com.ott.platform.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.data.model.Plan
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.components.OttTextButton
import com.ott.platform.ui.theme.LocalSpacing
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttGold
import com.ott.platform.ui.theme.OttSurfaceVariant
import com.ott.platform.ui.theme.OttTextSecondary


private val PlanRedBadge = Color(0xFFE50914)
private val PlanCardBorderSelected = Color(0xFFE50914)
private val PlanCardBorderUnselected = Color(0xFF262626)
private val PlanCardBgSelected = Color(0xFF181012)

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

    // Synchronize default selected plan when plans list loads
    LaunchedEffect(plans) {
        if (selectedPlan == null && plans.isNotEmpty()) {
            selectedPlan = plans.firstOrNull { it.featured } ?: plans.first()
        }
    }

    // Auto-navigate to main content when subscription becomes active
    LaunchedEffect(subscriptionState) {
        if (subscriptionState is SubscriptionState.Active) {
            onSubscribeSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(spacing.xl))

            // Brand Header matching website styling
            Text(
                text = "Subscribe And Enjoy",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = "Choose a plan for becoming Prime member",
                style = MaterialTheme.typography.bodyMedium,
                color = OttTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(spacing.lg))

            // Status feedback messages
            if (subscriptionState is SubscriptionState.Confirming || subscriptionState is SubscriptionState.Pending) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.md)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E1A05))
                        .border(1.dp, OttGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = OttGold,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(spacing.md))
                        Text(
                            text = "Activating subscription pass...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OttGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (subscriptionState is SubscriptionState.Error) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.md)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B1216))
                        .border(1.dp, Color(0xFFE50914).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (subscriptionState as SubscriptionState.Error).message,
                        color = Color(0xFFFF8080),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Plans List
            if (plans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PlanRedBadge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(plans, key = { it.id }) { plan ->
                        val isSelected = selectedPlan?.id == plan.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) PlanCardBgSelected else OttSurfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PlanCardBorderSelected else PlanCardBorderUnselected,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedPlan = plan }
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Top row: Duration Badge & Popular Tag
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Solid red duration pill badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PlanRedBadge)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = plan.formattedPeriod().uppercase(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    if (plan.featured) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(OttGold.copy(alpha = 0.15f))
                                                .border(1.dp, OttGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = OttGold,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "POPULAR",
                                                    color = OttGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Plan Title and Price
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = plan.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        ),
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = plan.formattedAmount(),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        color = PlanRedBadge
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Feature Checklist
                                val featuresToDisplay = if (plan.features.isNotEmpty()) {
                                    plan.features
                                } else {
                                    listOf(
                                        "Unlimited Streaming",
                                        "Watch Content In HD, 4K, 2K",
                                        "Watch Everywhere",
                                        "New Release In Every Week"
                                    )
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    featuresToDisplay.forEach { feature ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) PlanRedBadge else Color(0xFF888888))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = feature,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) Color(0xFFE0E0E0) else OttTextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            // Selected Plan Summary & Primary Action Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OttButton(
                    text = if (selectedPlan != null) {
                        "BUY NOW  •  ${selectedPlan!!.formattedAmount()} (${selectedPlan!!.formattedPeriod()})"
                    } else {
                        "BUY NOW"
                    },
                    onClick = { selectedPlan?.let { viewModel.initiatePayment(it) } },
                    enabled = !isPaymentProcessing && selectedPlan != null && subscriptionState !is SubscriptionState.Pending && subscriptionState !is SubscriptionState.Confirming,
                    isLoading = isPaymentProcessing
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                // Trust footer text matching web
                Text(
                    text = "Instant activation • Google Pay, PhonePe, Cards, UPI & Netbanking",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = OttTextSecondary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                OttTextButton(
                    text = "Not now",
                    onClick = onNavigateBack,
                    color = OttTextSecondary
                )
            }
        }
    }
}
