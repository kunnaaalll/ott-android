package com.ott.platform.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firestore: subscriptions/{subscriptionId}
 *
 * Written exclusively by Cloud Functions after payment verification.
 * The client only reads this document — never writes to it.
 */
data class Subscription(
    @DocumentId
    val id: String = "",

    @PropertyName("tenantId")
    val tenantId: String = "",

    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("planId")
    val planId: String = "",

    @PropertyName("gatewaySubscriptionId")
    val gatewaySubscriptionId: String = "",

    val status: String = SubscriptionStatus.PENDING.name,

    @PropertyName("startDate")
    val startDate: Timestamp? = null,

    @PropertyName("expiryDate")
    val expiryDate: Timestamp? = null,

    @ServerTimestamp
    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @PropertyName("updatedAt")
    val updatedAt: Timestamp? = null,
) {
    /**
     * Whether the subscription grants access right now.
     * Checks both status == ACTIVE and expiryDate > now.
     */
    fun isActive(): Boolean {
        val statusActive = SubscriptionStatus.fromString(status) == SubscriptionStatus.ACTIVE
        val notExpired = expiryDate?.let { it.toDate().time > System.currentTimeMillis() } ?: false
        return statusActive && notExpired
    }
}
