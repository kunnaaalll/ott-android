package com.ott.platform.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ott.platform.ui.theme.OttTextSecondary
import com.ott.platform.ui.theme.OttTextTertiary

/**
 * Full-screen error/empty states with icon, title, subtitle, and optional action.
 * Matches the spec requirement: "The application should always communicate what is happening."
 */
@Composable
fun ErrorState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = OttTextTertiary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = OttTextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = OttTextTertiary,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun NetworkErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorState(
        title = "Couldn't connect",
        subtitle = "Check your internet connection and try again.",
        icon = Icons.Outlined.CloudOff,
        modifier = modifier,
        action = {
            OttButton(
                text = "Retry",
                onClick = onRetry,
            )
        },
    )
}

@Composable
fun SubscriptionExpiredState(
    onRenew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ErrorState(
        title = "Your subscription has expired",
        subtitle = "Renew your subscription to continue watching.",
        icon = Icons.Outlined.LockClock,
        modifier = modifier,
        action = {
            OttButton(
                text = "Renew Subscription",
                onClick = onRenew,
            )
        },
    )
}

@Composable
fun EmptySearchState(modifier: Modifier = Modifier) {
    ErrorState(
        title = "No results found",
        subtitle = "Try searching with different keywords.",
        icon = Icons.Outlined.SearchOff,
        modifier = modifier,
    )
}

@Composable
fun EmptyListState(modifier: Modifier = Modifier) {
    ErrorState(
        title = "Your list is empty",
        subtitle = "Browse the catalogue and add titles you want to watch later.",
        icon = Icons.Outlined.SearchOff,
        modifier = modifier,
    )
}
