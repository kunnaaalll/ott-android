package com.ott.platform.core.analytics

import android.os.Bundle

/**
 * Interface for Meta (Facebook) App Events tracking.
 * Provides standard event contracts with deterministic deduplication support.
 */
interface MetaAnalyticsTracker {

    /**
     * Initializes the Meta SDK with privacy and consent guards.
     */
    fun initialize()

    /**
     * Updates privacy and advertising tracking consent state.
     */
    fun setTrackingConsent(trackingAllowed: Boolean)

    /**
     * Logs standard `fb_mobile_initiated_checkout` when user initiates a purchase flow.
     */
    fun logInitiateCheckout(
        planId: String,
        planName: String,
        amount: Double,
        currency: String = "INR",
        checkoutSessionId: String? = null
    )

    /**
     * Logs standard `fb_mobile_purchase` upon verified payment confirmation.
     * Enforces client-side idempotency using [orderId] as the authoritative `event_id`
     * to match Meta Conversions API (CAPI) events.
     */
    fun logPurchase(
        orderId: String,
        planId: String,
        planName: String,
        amount: Double,
        currency: String = "INR",
        extraParams: Bundle? = null
    ): Boolean

    /**
     * Checks if a purchase event for the given [orderId] has already been emitted.
     */
    fun isPurchaseAlreadyLogged(orderId: String): Boolean
}
