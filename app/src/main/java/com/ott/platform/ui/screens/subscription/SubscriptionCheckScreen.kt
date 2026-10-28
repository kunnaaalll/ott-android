package com.ott.platform.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttGold

@Composable
fun SubscriptionCheckScreen(
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onSubscriptionActive: () -> Unit,
    onSubscriptionInactive: () -> Unit
) {
    val subscriptionState by viewModel.subscriptionState.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    fun navigateOnce(action: () -> Unit) {
        if (!hasNavigated) {
            hasNavigated = true
            action()
        }
    }

    LaunchedEffect(subscriptionState) {
        when (subscriptionState) {
            is SubscriptionState.Active -> navigateOnce(onSubscriptionActive)
            is SubscriptionState.Inactive,
            is SubscriptionState.Expired,
            is SubscriptionState.Cancelled,
            is SubscriptionState.Error -> navigateOnce(onSubscriptionInactive)
            else -> {
                // Loading or Pending, stay on this screen and show loader
            }
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        if (subscriptionState is SubscriptionState.Loading || subscriptionState is SubscriptionState.Initial) {
            navigateOnce(onSubscriptionInactive)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = OttGold)
    }
}

