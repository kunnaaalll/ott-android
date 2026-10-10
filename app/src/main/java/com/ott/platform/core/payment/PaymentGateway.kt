package com.ott.platform.core.payment

import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.Subscription
import com.ott.platform.data.model.User

/**
 * Interface abstracting the underlying Payment Gateway.
 * 
 * IMPORTANT: The Android application must NOT contain payment gateway secret credentials.
 * The application initiates the payment process, but does not definitively "activate"
 * the subscription directly. The payment gateway verifies and communicates success
 * through a secure webhook to the Cloud Functions.
 */
interface PaymentGateway {
    
    /**
     * Initiates the payment process UI / intent for the given [plan] and [user].
     * 
     * @param user The current authenticated user.
     * @param plan The subscription plan the user wants to buy.
     */
    suspend fun initiatePayment(user: User, plan: Plan)
    
    /**
     * Retrieves the latest subscription status directly from the gateway if necessary.
     * (Normally, the app relies on Firestore's subscription document updated by webhooks).
     */
    suspend fun getSubscriptionStatus(gatewaySubscriptionId: String): String
    
    /**
     * Optional method for cancelling a subscription from the client, though
     * ideally handled via Cloud Functions.
     */
    suspend fun cancelSubscription(gatewaySubscriptionId: String)
}
