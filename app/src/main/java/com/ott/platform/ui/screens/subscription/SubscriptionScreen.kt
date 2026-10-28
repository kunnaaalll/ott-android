
package com.ott.platform.ui.screens.subscription

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
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
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    // Open the website checkout (already signed in through the single-use handoff) in a Custom Tab.
    LaunchedEffect(viewModel) {
        viewModel.checkoutLaunch.collect { url -> openCheckoutInBrowser(context, url) }
    }

    // Coming back from the browser is only a navigation event: re-read the state from the backend.
    if (lifecycleOwner != null) {
        DisposableEffect(lifecycleOwner, viewModel) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    try {
                        viewModel.onScreenResumed()
                    } catch (t: Throwable) {
                        // Safe fallback on unexpected state
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    var selectedPlan by remember { mutableStateOf<Plan?>(null) }
    var hasHandledSuccess by remember { mutableStateOf(false) }

    // Synchronize default selected plan when plans list loads (lowest plan on top selected first)
    LaunchedEffect(plans) {
        if (selectedPlan == null && plans.isNotEmpty()) {
            selectedPlan = plans.first()
        }
    }

    // Auto-navigate to main content when subscription becomes active
    LaunchedEffect(subscriptionState) {
        if (subscriptionState is SubscriptionState.Active && !hasHandledSuccess) {
            hasHandledSuccess = true
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

            if (subscriptionState is SubscriptionState.ConfirmingTimeout) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.md)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E2005))
                        .border(1.dp, OttGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(spacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Payment confirmation is taking a moment. If you completed payment in your UPI app, check status below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = OttGold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OttButton(
                            text = "CHECK PAYMENT STATUS",
                            onClick = { viewModel.retryCheckPayment() }
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
                    itemsIndexed(plans, key = { index, plan -> "${plan.id.ifBlank { "plan" }}_$index" }) { _, plan ->
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
                                val featuresToDisplay = plan.features?.takeIf { it.isNotEmpty() } ?: listOf(
                                    "Unlimited Streaming",
                                    "Watch Content In HD, 4K, 2K",
                                    "Watch Everywhere",
                                    "New Release In Every Week"
                                )

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
                val currentPlan = selectedPlan
                OttButton(
                    text = if (currentPlan != null) {
                        "BUY NOW  •  ${currentPlan.formattedAmount()} (${currentPlan.formattedPeriod()})"
                    } else {
                        "BUY NOW"
                    },
                    onClick = { currentPlan?.let { viewModel.initiatePayment(it) } },
                    enabled = !isPaymentProcessing && currentPlan != null && subscriptionState !is SubscriptionState.Pending && subscriptionState !is SubscriptionState.Confirming,
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

/**
 * Opens the checkout URL in a Custom Tab (falls back to the default browser). The URL contains a
 * one-time secret in its fragment, so it is never logged.
 */
private fun openCheckoutInBrowser(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        val tab = CustomTabsIntent.Builder().setShowTitle(true).build()
        tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tab.launchUrl(context, uri)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: ActivityNotFoundException) {
            // No browser available on the device; nothing else to do.
        }
    }
}
