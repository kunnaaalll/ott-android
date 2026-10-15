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

    @PropertyName("status")
    val status: String = SubscriptionStatus.PENDING.name,

    @PropertyName("currentPeriodStart")
    val currentPeriodStart: Long? = null,

    @PropertyName("currentPeriodEnd")
    val currentPeriodEnd: Long? = null,

    @PropertyName("cancelAtPeriodEnd")
    val cancelAtPeriodEnd: Boolean = false,

    @PropertyName("gracePeriodEnd")
    val gracePeriodEnd: Long? = null,

    @PropertyName("provider")
    val provider: String? = null,

    @PropertyName("providerSubscriptionId")
    val providerSubscriptionId: String? = null,

    @PropertyName("lastVerifiedAt")
    val lastVerifiedAt: Long? = null,

    // Legacy fields for backward compatibility
    @PropertyName("gatewaySubscriptionId")
    val gatewaySubscriptionId: String = "",

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
     * Client-side presentation helper.
     * Evaluates access according to canonical rules:
     * - ACTIVE: currentPeriodEnd > now (or legacy expiryDate > now)
     * - CANCELLED: currentPeriodEnd > now (if cancelAtPeriodEnd is true)
     * - GRACE_PERIOD: gracePeriodEnd > now
     * - All other states: false
     *
     * Note: Authorization authority strictly resides in backend Cloud Functions.
     */
    fun isActive(): Boolean {
        val now = System.currentTimeMillis()
        val canonicalEnd = currentPeriodEnd ?: expiryDate?.toDate()?.time ?: return false

        return when (SubscriptionStatus.fromString(status)) {
            SubscriptionStatus.ACTIVE -> canonicalEnd > now
            SubscriptionStatus.CANCELLED -> cancelAtPeriodEnd && canonicalEnd > now
            SubscriptionStatus.GRACE_PERIOD -> {
                val graceEnd = gracePeriodEnd ?: return false
                graceEnd > now
            }
            else -> false
        }
    }
}
