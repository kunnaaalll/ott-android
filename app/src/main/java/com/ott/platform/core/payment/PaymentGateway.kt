package com.ott.platform.core.payment

import com.ott.platform.core.api.CheckoutHandoff
import com.ott.platform.data.model.Plan

/**
 * Payments are handled entirely by the Chumbak website and backend (Cashfree hosted checkout).
 *
 * IMPORTANT: The Android application must NOT contain payment gateway credentials, call the
 * gateway's APIs, or embed a gateway SDK. The app only asks the backend for a single-use
 * checkout handoff, opens the returned website URL in the browser, and afterwards reads the
 * user's subscription state from the backend. Subscriptions are activated server-side after
 * verified payment (order verification and signed gateway webhooks).
 */
interface PaymentGateway {

    /**
     * Requests a single-use website checkout handoff for [plan]. The returned URL opens the
     * Chumbak checkout with the user already signed in. It authenticates the user only and
     * never grants paid access.
     *
     * @throws com.ott.platform.core.api.NotSignedInException if the app session is no longer valid.
     */
    suspend fun startWebCheckout(plan: Plan): CheckoutHandoff

    /**
     * Retrieves the backend-verified status of an order ("ACTIVE" only when the backend confirms
     * payment, otherwise "PENDING").
     */
    suspend fun getSubscriptionStatus(gatewaySubscriptionId: String): String

    /**
     * Optional method for cancelling a subscription from the client, though
     * ideally handled server-side.
     */
    suspend fun cancelSubscription(gatewaySubscriptionId: String)
}
