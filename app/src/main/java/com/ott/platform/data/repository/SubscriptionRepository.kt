package com.ott.platform.data.repository

import com.ott.platform.data.model.Plan
import com.ott.platform.data.model.Subscription
import kotlinx.coroutines.flow.Flow

/**
 * Subscription & plan operations.
 *
 * IMPORTANT: This repository only reads subscription data.
 * Subscription creation/mutation happens via Cloud Functions triggered
 * by the payment gateway webhook — never by the client directly.
 */
interface SubscriptionRepository {

    /** Observe the active subscription for a user. */
    fun observeSubscription(userId: String, tenantId: String): Flow<Subscription?>

    /** Get all available plans for the tenant. */
    suspend fun getPlans(tenantId: String): List<Plan>

    /** Check whether the user currently has an active subscription. */
    suspend fun isSubscriptionActive(userId: String, tenantId: String): Boolean
}
