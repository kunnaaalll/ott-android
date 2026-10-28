package com.ott.platform.core.payment

import com.ott.platform.core.analytics.MetaAnalyticsTracker
import com.ott.platform.core.api.CheckoutHandoff
import com.ott.platform.core.api.OrderStatus
import com.ott.platform.core.api.WebsiteApiClient
import com.ott.platform.core.session.SessionManager
import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.SubscriptionStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Web-based checkout adapter. The app never contacts the payment gateway directly:
 * 1. [startWebCheckout] asks the Chumbak backend for a checkout session.
 * 2. If the single-use handoff endpoint is unavailable, it seamlessly uses the authoritative
 *    Cashfree order creation endpoint on thechumbak.com.
 * 3. The caller opens the returned website URL in the browser, where the user completes payment.
 * 4. Subscriptions are activated by the backend after verified payment.
 */
@Singleton
class ExternalPaymentGatewayAdapter @Inject constructor(
    private val websiteApiClient: WebsiteApiClient,
    private val metaAnalyticsTracker: MetaAnalyticsTracker,
    private val sessionManager: SessionManager
) : PaymentGateway {

    override suspend fun startWebCheckout(plan: Plan): CheckoutHandoff {
        val user = sessionManager.getSessionUser()
        val userId = user?.id ?: sessionManager.getUserId() ?: "usr_mobile_app"
        val phone = sessionManager.getPhoneNumber() ?: ""
        val email = user?.email ?: ""

        val handoffResult = runCatching { websiteApiClient.requestCheckoutHandoff(plan.id).getOrThrow() }
        val handoff = if (handoffResult.isSuccess) {
            handoffResult.getOrThrow()
        } else {
            websiteApiClient.createCashfreeOrder(
                planId = plan.id,
                amount = plan.price,
                userId = userId,
                customerPhone = phone,
                customerEmail = email
            ).getOrThrow()
        }

        // Track InitiateCheckout
        metaAnalyticsTracker.logInitiateCheckout(
            planId = plan.id,
            planName = plan.name,
            amount = plan.price,
            currency = "INR",
            checkoutSessionId = null
        )
        return handoff
    }

    override suspend fun getSubscriptionStatus(gatewaySubscriptionId: String): String {
        return try {
            val status = websiteApiClient.checkOrderStatus(gatewaySubscriptionId).getOrNull()
            if (status == OrderStatus.PAID) SubscriptionStatus.ACTIVE.name else SubscriptionStatus.PENDING.name
        } catch (e: Exception) {
            SubscriptionStatus.PENDING.name
        }
    }

    override suspend fun cancelSubscription(gatewaySubscriptionId: String) {
        // Authoritative cancellation is handled server-side.
    }
}
