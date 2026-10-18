package com.ott.platform.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ott.platform.ui.components.OttButton
import com.ott.platform.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OttBackground)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OttGold)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = spacing.screenPadding)
            ) {
                Spacer(modifier = Modifier.height(spacing.md))

                // ── Screen Header ──────────────────────────────────────────
                Text(
                    text = "Profile & Account",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OttGold,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Manage your Chumbak OTT account, subscription & settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = OttTextSecondary
                )

                Spacer(modifier = Modifier.height(spacing.lg))

            // ── User Identity Hero Card ────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = OttSurfaceVariant,
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(OttGold.copy(alpha = 0.35f), OttBorder)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initial = uiState.name.firstOrNull()?.uppercase()
                        ?: uiState.email.firstOrNull()?.uppercase()
                        ?: "U"

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(OttGold, ChumbakAmber, ChumbakAmberLight, OttGold)
                                )
                            )
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF20180D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = OttGold,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.name.ifBlank { "Chumbak Member" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OttTextPrimary
                        )

                        if (uiState.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = OttGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.phone,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OttTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (uiState.email.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = OttTextTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OttTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (uiState.hasActiveSubscription) OttGold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (uiState.hasActiveSubscription) OttGold.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (uiState.hasActiveSubscription) Icons.Default.Star else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (uiState.hasActiveSubscription) OttGold else OttTextSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.hasActiveSubscription) "VIP PASS ACTIVE" else "FREE TIER",
                                        color = if (uiState.hasActiveSubscription) OttGold else OttTextSecondary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (uiState.isAdmin) {
                                Surface(
                                    color = OttRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, OttRed.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "ADMIN",
                                        color = OttRed,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // ── VIP Subscription Pass Card ─────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(OttGold.copy(alpha = 0.5f), Color(0xFF8C6615), OttGold.copy(alpha = 0.25f))
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF261B0B),
                                    Color(0xFF1B1612),
                                    Color(0xFF141315)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(OttGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = OttGold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "CHUMBAK VIP PASS",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                                        color = OttGold,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = uiState.planName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            val badgeColor = if (uiState.hasActiveSubscription) Color(0xFF10B981) else Color(0xFFEF4444)
                            Surface(
                                color = badgeColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = uiState.subscriptionStatus.uppercase(),
                                        color = badgeColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (uiState.expiryDate != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Valid until: ${uiState.expiryDate} • Unlimited Streaming Access",
                                style = MaterialTheme.typography.bodySmall,
                                color = OttTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Included VIP Perks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            VipPerkItem(label = "4K UHD")
                            VipPerkItem(label = "100% Ad-Free")
                            VipPerkItem(label = "Multi-Device")
                            VipPerkItem(label = "Downloads")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OttButton(
                            text = if (uiState.hasActiveSubscription) "Change / Renew Subscription" else "Upgrade to Premium VIP",
                            onClick = onNavigateToSubscription,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            // ── Section: Playback & Streaming ──────────────────────────
            ProfileSectionHeader(title = "Playback & Streaming")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = OttSurfaceVariant
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileClickableRow(
                        icon = Icons.Default.Settings,
                        title = "Streaming Quality",
                        subtitle = "Preferred video resolution for playback",
                        value = uiState.streamingQuality,
                        onClick = { showQualityDialog = true }
                    )
                    HorizontalDivider(color = OttDivider, thickness = 1.dp)
                    ProfileSwitchRow(
                        icon = Icons.Default.Wifi,
                        title = "Stream on Wi-Fi Only",
                        subtitle = "Conserve cellular mobile data usage",
                        checked = uiState.wifiOnlyStreaming,
                        onCheckedChange = { viewModel.setWifiOnlyStreaming(it) }
                    )
                    HorizontalDivider(color = OttDivider, thickness = 1.dp)
                    ProfileSwitchRow(
                        icon = Icons.Default.PlayArrow,
                        title = "Autoplay Previews",
                        subtitle = "Play video previews while browsing titles",
                        checked = uiState.autoplayPreviews,
                        onCheckedChange = { viewModel.setAutoplayPreviews(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // ── Section: Downloads & Storage ───────────────────────────
            ProfileSectionHeader(title = "Downloads & Storage")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = OttSurfaceVariant
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileSwitchRow(
                        icon = Icons.Default.Download,
                        title = "Download over Wi-Fi Only",
                        subtitle = "Avoid unexpected carrier data charges",
                        checked = uiState.wifiOnlyDownloads,
                        onCheckedChange = { viewModel.setWifiOnlyDownloads(it) }
                    )
                    HorizontalDivider(color = OttDivider, thickness = 1.dp)
                    ProfileClickableRow(
                        icon = Icons.Default.Delete,
                        iconTint = OttError,
                        title = "Clear Cached Media",
                        subtitle = "Free up device storage (${uiState.cacheSize} cached)",
                        value = "Clear",
                        onClick = {
                            viewModel.clearCache()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Temporary playback cache cleared successfully")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // ── Section: Notifications & Language ──────────────────────
            ProfileSectionHeader(title = "Notifications & Display")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = OttSurfaceVariant
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = "Push Notifications",
                        subtitle = "New releases, blockbuster movies & recommendations",
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                    HorizontalDivider(color = OttDivider, thickness = 1.dp)
                    ProfileClickableRow(
                        icon = Icons.Default.Language,
                        title = "App Language",
                        subtitle = "Audio & subtitle default preferences",
                        value = "English (India)",
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Default language set to English (India)")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.lg))

            // ── Section: Help, Legal & Version ─────────────────────────
            ProfileSectionHeader(title = "Help & Support")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = OttSurfaceVariant
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileClickableRow(
                        icon = Icons.Default.Help,
                        title = "Help Center & FAQs",
                        subtitle = "Contact customer care, billing & playback help",
                        onClick = { showSupportDialog = true }
                    )
                    HorizontalDivider(color = OttDivider, thickness = 1.dp)
                    ProfileClickableRow(
                        icon = Icons.Default.Security,
                        title = "Privacy Policy & Terms",
                        subtitle = "Terms of service, end user agreement & privacy",
                        onClick = {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Visit thechumbak.com/terms for full terms of service")
                            }
                        }
                    )
                    HorizontalDivider(color = OttDivider, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = OttGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "App Version",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OttTextPrimary
                                )
                                Text(
                                    text = "Chumbak OTT v1.2.0 • Build 2026.10",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OttTextSecondary
                                )
                            }
                        }
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "LATEST",
                                color = Color(0xFF10B981),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.xl))

            // ── Sign Out CTA ───────────────────────────────────────────
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OttError,
                    containerColor = OttError.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, OttError.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = OttError,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = OttError
                )
            }

            // Comfortable clearance above bottom navigation
            Spacer(modifier = Modifier.height(spacing.bottomNavHeight + 40.dp))
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = spacing.bottomNavHeight + 16.dp)
    )
}

    // ── Dialog: Sign Out Confirmation ──────────────────────────────────
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = OttSurfaceElevated,
            title = {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of Chumbak OTT? You will need to sign back in with your mobile number to stream videos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OttTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OttError)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = OttTextSecondary)
                }
            }
        )
    }

    // ── Dialog: Streaming Quality Selection ────────────────────────────
    if (showQualityDialog) {
        val qualities = listOf(
            "Auto (Adaptive - Recommended)",
            "Full HD (1080p)",
            "Data Saver (480p)"
        )
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = OttSurfaceElevated,
            title = {
                Text(
                    text = "Streaming Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary
                )
            },
            text = {
                Column {
                    qualities.forEach { quality ->
                        val isSelected = uiState.streamingQuality.startsWith(quality.take(7))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setStreamingQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = quality,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) OttGold else OttTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = OttGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Done", color = OttGold, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ── Dialog: Help & Support ─────────────────────────────────────────
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            containerColor = OttSurfaceElevated,
            title = {
                Text(
                    text = "Chumbak Support",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OttTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "For customer support, membership inquiries or playback issues, our support team is available 24/7:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OttTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Email: support@thechumbak.com\n• Website: https://thechumbak.com\n• Response Time: Within 24 hours",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OttTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSupportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OttGold)
                ) {
                    Text("Got It", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color = OttGold,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun VipPerkItem(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = OttGold,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProfileClickableRow(
    icon: ImageVector,
    iconTint: Color = OttGold,
    title: String,
    subtitle: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OttTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OttTextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OttGold,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = OttTextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OttGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OttTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OttTextSecondary
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OttGold,
                checkedTrackColor = OttGold.copy(alpha = 0.35f),
                uncheckedThumbColor = OttTextTertiary,
                uncheckedTrackColor = OttBorder
            )
        )
    }
}
