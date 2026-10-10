package com.ott.platform.core.payment

import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.User
import javax.inject.Inject
import javax.inject.Singleton

class PaymentGatewayNotConfiguredException(message: String) : Exception(message)

/**
 * Adapter for the external Payment Gateway.
 *
 * REQUIRED INFORMATION TO COMPLETE INTEGRATION:
 * 1. The SDK dependencies (e.g., Stripe, Razorpay, Braintree SDKs).
 * 2. Public client keys (NOT secret keys) to initialize the SDK.
 * 3. The exact UI flow (whether it launches an Activity, a Custom Tab, or an in-app bottom sheet).
 * 4. The webhook event schema for the Cloud Function to parse.
 */
@Singleton
class ExternalPaymentGatewayAdapter @Inject constructor() : PaymentGateway {

    override suspend fun initiatePayment(user: User, plan: Plan) {
        // EXPLICIT FAILURE: Do not fake a successful intent launch.
        throw PaymentGatewayNotConfiguredException("Payment provider is not configured.")
    }

    override suspend fun getSubscriptionStatus(gatewaySubscriptionId: String): String {
        throw PaymentGatewayNotConfiguredException("Payment provider is not configured.")
    }

    override suspend fun cancelSubscription(gatewaySubscriptionId: String) {
        throw PaymentGatewayNotConfiguredException("Payment provider is not configured.")
    }
}
