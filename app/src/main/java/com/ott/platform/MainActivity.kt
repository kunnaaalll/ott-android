package com.ott.platform

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.ott.platform.core.analytics.MetaAnalyticsTracker
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.core.payment.CheckoutConfirmation
import com.ott.platform.core.payment.CheckoutConfirmationService
import com.ott.platform.ui.navigation.MainNavGraph
import com.ott.platform.ui.theme.OttBackground
import com.ott.platform.ui.theme.OttTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var websiteApiClient: WebsiteApiClient

    @Inject
    lateinit var metaAnalyticsTracker: MetaAnalyticsTracker

    @Inject
    lateinit var subscriptionRepository: com.ott.platform.data.repository.SubscriptionRepository

    @Inject
    lateinit var sessionManager: com.ott.platform.core.session.SessionManager

    @Inject
    lateinit var checkoutConfirmation: CheckoutConfirmationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        syncSubscriptionWithBackend()

        setContent {
            OttTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OttBackground
                ) {
                    MainNavGraph()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncSubscriptionWithBackend()
    }

    private fun syncSubscriptionWithBackend() {
        if (!sessionManager.isLoggedIn()) return
        lifecycleScope.launch {
            try {
                val user = sessionManager.getSessionUser()
                val userId = sessionManager.getUserId() ?: ""
                val email = user?.email ?: ""
                val mobile = sessionManager.getPhoneNumber() ?: ""

                val result = websiteApiClient.fetchSubscription(userId, email, mobile)
                if (result.isSuccess) {
                    val sub = result.getOrNull()
                    if (sub != null) {
                        subscriptionRepository.recordActiveSubscription(sub)
                    }
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to sync subscription on resume: ${e.message}")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * Return link from the website checkout (chumbak://subscription/status?order_id=... or the
     * https pricing/status page). The link is only a navigation event: anyone can craft one, so it
     * never changes subscription state. The app asks the backend, which verifies payment with the
     * gateway server-side, and shows whatever the backend reports.
     */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val isCheckoutReturn =
            (data.scheme == "chumbak" && data.host == "subscription") ||
                (data.scheme == "https" && data.host == "thechumbak.com" && data.path?.startsWith("/pricing/status") == true)
        if (!isCheckoutReturn) return

        val orderId = (data.getQueryParameter("order_id") ?: data.getQueryParameter("orderId"))
            ?.takeIf { ORDER_ID_PATTERN.matches(it) }
        if (!sessionManager.isLoggedIn()) return

        lifecycleScope.launch {
            try {
                val outcome = checkoutConfirmation.confirm(orderId, maxAttempts = 6, delayMs = 2000)
                if (outcome is CheckoutConfirmation.Confirmed && orderId != null) {
                    // Authoritative purchase tracking is strictly handled by the backend Server
                    // Conversions API (CAPI) upon Cashfree payment confirmation.
                    // Client-side emission is disabled to prevent duplicate or phantom events.
                    Log.i("MainActivity", "Subscription confirmed for order $orderId; tracking managed by backend CAPI.")
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Could not confirm checkout with backend: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        val ORDER_ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
